package org.iosclone.keyboard.gesture

import org.iosclone.keyboard.dictionary.Trie
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.layout.KeyboardLayout
import kotlin.math.hypot

class GesturePathMatcher {

    /**
     * Matches a gesture point sequence against the keyboard layout and trie.
     */
    fun match(
        points: List<GesturePoint>,
        layout: KeyboardLayout,
        trie: Trie?
    ): List<String> {
        if (points.size < 4 || trie == null) return emptyList()

        // 1. Identify start and end keys
        val firstPt = points.first()
        val lastPt = points.last()

        val startKey = layout.findKeyAt(firstPt.x, firstPt.y) ?: return emptyList()
        val endKey = layout.findKeyAt(lastPt.x, lastPt.y) ?: return emptyList()

        val startChar = startKey.label.lowercase().firstOrNull() ?: return emptyList()
        val endChar = endKey.label.lowercase().firstOrNull() ?: return emptyList()

        // 2. Identify key sequence traversed along the path
        val traversedKeys = mutableListOf<KeyDefinition>()
        var previousKey: KeyDefinition? = null

        for (pt in points) {
            val key = layout.findKeyAt(pt.x, pt.y)
            if (key != null && key != previousKey && key.label.length == 1 && key.label[0].isLetter()) {
                traversedKeys.add(key)
                previousKey = key
            }
        }

        if (traversedKeys.size < 2) return emptyList()

        val traversedChars = traversedKeys.map { it.label.lowercase()[0] }

        // 3. Find prefix suggestions starting with startChar
        val candidates = trie.findPrefixSuggestions(startChar.toString(), limit = 60)

        // 4. Score candidates
        val scored = mutableListOf<Pair<String, Float>>()

        for ((word, freq) in candidates) {
            val lowerWord = word.lowercase()
            if (lowerWord.length < 2) continue
            // Must end with endChar (or close to endChar)
            if (lowerWord.last() != endChar) continue

            // Check if characters of word appear in order in the traversed path
            var pathIdx = 0
            var matchedLetters = 0
            for (ch in lowerWord) {
                while (pathIdx < traversedChars.size) {
                    if (traversedChars[pathIdx] == ch) {
                        matchedLetters++
                        pathIdx++
                        break
                    }
                    pathIdx++
                }
            }

            if (matchedLetters == lowerWord.length) {
                // Perfect order match! Score is based on frequency and length closeness
                val lengthDiff = kotlin.math.abs(traversedChars.size - lowerWord.length)
                val score = (freq * 2.0f) - (lengthDiff * 10f)
                scored.add(Pair(word, score))
            } else if (matchedLetters >= lowerWord.length - 1 && lowerWord.length >= 4) {
                val score = freq * 0.5f
                scored.add(Pair(word, score))
            }
        }

        return scored.sortedByDescending { it.second }.map { it.first }.take(3)
    }
}
