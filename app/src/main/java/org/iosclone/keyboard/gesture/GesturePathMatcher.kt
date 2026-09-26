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

    fun match(
        points: List<GesturePoint>,
        layout: KeyboardLayout,
        trie: Trie?
    ): List<String> {
        if (points.size < 3) return emptyList()

        val isSymbolOrNumeric = (layout.mode == org.iosclone.keyboard.layout.KeyboardMode.NUMERIC ||
                layout.mode == org.iosclone.keyboard.layout.KeyboardMode.SYMBOL)

        // 1. Identify start and end character keys (with nearest-key tolerance)
        val firstPt = points.first()
        val lastPt = points.last()

        val startKey = findNearestCharacterKey(firstPt.x, firstPt.y, layout) ?: return emptyList()
        val endKey = findNearestCharacterKey(lastPt.x, lastPt.y, layout) ?: return emptyList()

        val startChar = startKey.label.firstOrNull() ?: return emptyList()
        val endChar = endKey.label.firstOrNull() ?: return emptyList()

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

        // 3. Query dictionary trie for words starting with startChar (and nearby neighbors)
        val candidateWords = mutableMapOf<String, Int>()
        val startCandidates = trie.findPrefixSuggestions(lowerStartChar.toString(), limit = 120)
        for ((word, freq) in startCandidates) {
            candidateWords[word] = freq
        }

        // 4. Score candidates
        data class ScoredWord(val word: String, val score: Float)
        val scoredList = mutableListOf<ScoredWord>()

        for ((word, freq) in candidateWords) {
            val lower = word.lowercase().trim()
            if (lower.length < 2) continue

            // Bonus for matching expected end character
            val endMatches = (lower.last() == lowerEndChar)
            val endBonus = if (endMatches) 70f else -30f

            // Check subsequence alignment
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
            if (matchRatio < 0.65f) continue

            val lengthDiff = abs(traversedLowerChars.size - lower.length)
            val score = (freq * 0.45f) + (matchRatio * 100f) + endBonus - (lengthDiff * 4f)
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
