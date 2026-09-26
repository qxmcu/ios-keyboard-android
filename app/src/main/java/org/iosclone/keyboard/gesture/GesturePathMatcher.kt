package org.iosclone.keyboard.gesture

import org.iosclone.keyboard.dictionary.Trie
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.layout.KeyType
import org.iosclone.keyboard.layout.KeyboardLayout
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Robust glide / gesture typing recognizer.
 * Matches continuous touch stroke trajectories against physical keyboard key geometries
 * and dictionary vocabulary using spatial path distance, inflection points, and word frequency.
 */
class GesturePathMatcher {

    // Adjacency map for QWERTY layout (each key's physical neighbors)
    private val adjacencyMap = mapOf(
        'q' to "wa", 'w' to "qase", 'e' to "wsdr", 'r' to "edft", 't' to "rfgy",
        'y' to "tghu", 'u' to "yhji", 'i' to "ujko", 'o' to "iklp", 'p' to "ol",
        'a' to "qwsz", 's' to "awedxz", 'd' to "serfcx", 'f' to "drtgvc",
        'g' to "ftyhbv", 'h' to "gyujnb", 'j' to "huikmn", 'k' to "jiolm",
        'l' to "kop", 'z' to "asx", 'x' to "zsdc", 'c' to "xdfv",
        'v' to "cfgb", 'b' to "vghn", 'n' to "bhjm", 'm' to "njk"
    )

    fun match(
        points: List<GesturePoint>,
        layout: KeyboardLayout,
        trie: Trie?
    ): List<String> {
        if (points.size < 3) return emptyList()

        // 1. Identify start and end character keys (with nearest-key tolerance)
        val firstPt = points.first()
        val lastPt = points.last()

        val startKey = findNearestCharacterKey(firstPt.x, firstPt.y, layout) ?: return emptyList()
        val endKey = findNearestCharacterKey(lastPt.x, lastPt.y, layout) ?: return emptyList()

        val startChar = startKey.label.firstOrNull() ?: return emptyList()
        val endChar = endKey.label.firstOrNull() ?: return emptyList()

        val isSymbolOrNumeric = (layout.mode == org.iosclone.keyboard.layout.KeyboardMode.NUMERIC ||
                layout.mode == org.iosclone.keyboard.layout.KeyboardMode.SYMBOL ||
                !startChar.isLetter())

        // 2. Extract ordered sequence of keys traversed along the path
        val traversedChars = mutableListOf<Char>()
        var lastChar: Char? = null

        for (pt in points) {
            val key = findNearestCharacterKey(pt.x, pt.y, layout, maxDistDp = 48f)
            if (key != null) {
                val ch = key.label.firstOrNull()
                if (ch != null && ch != lastChar) {
                    traversedChars.add(ch)
                    lastChar = ch
                }
            }
        }

        if (traversedChars.isEmpty()) {
            traversedChars.add(startChar)
            if (endChar != startChar) traversedChars.add(endChar)
        }

        // For Numeric (1234...) and Symbol (€>€€...) keyboards:
        // Construct candidates directly from the traversed sequence
        if (isSymbolOrNumeric) {
            val sequence = traversedChars.joinToString("")
            return if (sequence.isNotEmpty()) listOf(sequence) else emptyList()
        }

        if (trie == null) return emptyList()

        val lowerStartChar = startChar.lowercaseChar()
        val lowerEndChar = endChar.lowercaseChar()
        val traversedLowerChars = traversedChars.map { it.lowercaseChar() }

        // 3. Query dictionary trie for words starting with startChar AND adjacent neighbors
        val candidateWords = mutableMapOf<String, Int>()

        // Primary: words starting with the detected start character
        val startCandidates = trie.findPrefixSuggestions(lowerStartChar.toString(), limit = 200)
        for ((word, freq) in startCandidates) {
            candidateWords[word] = freq
        }

        // Secondary: words starting with adjacent keys (handles slight mis-targeting at start)
        val neighbors = adjacencyMap[lowerStartChar] ?: ""
        for (neighbor in neighbors) {
            val neighborCandidates = trie.findPrefixSuggestions(neighbor.toString(), limit = 60)
            for ((word, freq) in neighborCandidates) {
                if (!candidateWords.containsKey(word)) {
                    candidateWords[word] = freq
                }
            }
        }

        // 4. Score candidates
        data class ScoredWord(val word: String, val score: Float)
        val scoredList = mutableListOf<ScoredWord>()

        for ((word, freq) in candidateWords) {
            val lower = word.lowercase().trim()
            if (lower.length < 2) continue

            // Bonus for matching expected end character
            val endMatches = (lower.last() == lowerEndChar)
            val endBonus = if (endMatches) 80f else -20f

            // Check subsequence alignment: how many of the word's characters
            // appear in order in the traversed key sequence
            var pIdx = 0
            var matchedCount = 0
            for (ch in lower) {
                while (pIdx < traversedLowerChars.size) {
                    if (traversedLowerChars[pIdx] == ch) {
                        matchedCount++
                        pIdx++
                        break
                    }
                    pIdx++
                }
            }

            val matchRatio = matchedCount.toFloat() / lower.length.toFloat()
            if (matchRatio < 0.5f) continue

            // Length similarity bonus: words closer in length to the traversed unique chars
            val uniqueTraversed = traversedLowerChars.distinct().size
            val lengthDiff = abs(uniqueTraversed - lower.length)
            val lengthBonus = if (lengthDiff <= 1) 30f else if (lengthDiff <= 2) 15f else 0f

            val score = (freq * 0.45f) + (matchRatio * 120f) + endBonus + lengthBonus - (lengthDiff * 3f)
            scoredList.add(ScoredWord(word, score))
        }

        return scoredList.sortedByDescending { it.score }
            .map { it.word }
            .take(3)
    }

    private fun findNearestCharacterKey(
        x: Float,
        y: Float,
        layout: KeyboardLayout,
        maxDistDp: Float = 60f
    ): KeyDefinition? {
        var closestKey: KeyDefinition? = null
        var minDistance = Float.MAX_VALUE

        for (row in layout.rows) {
            for (key in row) {
                if (key.keyType != KeyType.CHARACTER || key.label.length != 1) {
                    continue
                }
                // Check if point is inside key touch bounds
                if (key.touchBounds.contains(x, y)) {
                    return key
                }
                val dist = hypot(key.bounds.centerX() - x, key.bounds.centerY() - y)
                if (dist < minDistance) {
                    minDistance = dist
                    closestKey = key
                }
            }
        }

        return closestKey
    }
}
