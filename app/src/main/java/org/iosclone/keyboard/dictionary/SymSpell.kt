package org.iosclone.keyboard.dictionary

import kotlin.math.abs
import kotlin.math.min

/**
 * Fast Symmetric Delete Spelling Correction algorithm (SymSpell).
 * Pre-generates string deletions for dictionary words to enable O(1) dictionary lookups
 * within edit distance 1 and 2, delivering sub-millisecond autocorrection lookups.
 */
class SymSpell(
    val maxDictionaryEditDistance: Int = 2,
    val prefixLength: Int = 7
) {

    data class SuggestItem(
        val term: String,
        var distance: Int,
        var count: Long
    ) : Comparable<SuggestItem> {
        override fun compareTo(other: SuggestItem): Int {
            if (this.distance != other.distance) return this.distance.compareTo(other.distance)
            return other.count.compareTo(this.count)
        }
    }

    // Word -> Frequency
    private val words = HashMap<String, Long>(50000)

    // Delete String -> List of Suggested Words
    private val deletes = HashMap<String, MutableList<String>>(100000)

    val wordCount: Int
        get() = words.size

    /**
     * Inserts a word and pre-calculates its symmetric delete variants.
     */
    fun createDictionaryEntry(key: String, count: Long): Boolean {
        if (count <= 0) return false
        val word = key.lowercase().trim()
        if (word.isEmpty()) return false

        val currentCount = words[word]
        if (currentCount != null) {
            words[word] = currentCount + count
            return false
        }

        words[word] = count

        // Generate deletes up to maxDictionaryEditDistance
        val wordDeletes = editsPrefix(word)
        for (del in wordDeletes) {
            val suggestions = deletes.getOrPut(del) { ArrayList(2) }
            if (!suggestions.contains(word)) {
                suggestions.add(word)
            }
        }
        return true
    }

    /**
     * Performs lightning-fast candidate lookup using symmetric deletes.
     */
    fun lookup(
        input: String,
        maxEditDistance: Int = maxDictionaryEditDistance,
        limit: Int = 10
    ): List<SuggestItem> {
        val cleanInput = input.lowercase().trim()
        val inputLen = cleanInput.length

        if (inputLen == 0) return emptyList()

        val items = ArrayList<SuggestItem>()
        val consideredDeletes = HashSet<String>()
        val consideredSuggestions = HashSet<String>()

        // 1. Check exact match
        val exactCount = words[cleanInput]
        if (exactCount != null) {
            items.add(SuggestItem(cleanInput, 0, exactCount))
            if (maxEditDistance == 0) return items
        }

        consideredSuggestions.add(cleanInput)

        // 2. Generate deletes of the input string
        val candidates = editsPrefix(cleanInput)

        for (candidate in candidates) {
            if (!consideredDeletes.add(candidate)) continue

            val matchingWords = deletes[candidate] ?: continue
            for (suggestion in matchingWords) {
                if (!consideredSuggestions.add(suggestion)) continue

                val sugLen = suggestion.length
                if (abs(sugLen - inputLen) > maxEditDistance) continue

                // Verify true Levenshtein distance
                val dist = damerauLevenshteinDistance(cleanInput, suggestion, maxEditDistance)
                if (dist in 0..maxEditDistance) {
                    val count = words[suggestion] ?: 1L
                    items.add(SuggestItem(suggestion, dist, count))
                }
            }
        }

        items.sort()
        return if (items.size > limit) items.subList(0, limit) else items
    }

    private fun editsPrefix(key: String): Set<String> {
        val result = HashSet<String>()
        val prefix = if (key.length > prefixLength) key.substring(0, prefixLength) else key
        result.add(prefix)
        edits(prefix, 0, result)
        return result
    }

    private fun edits(word: String, editDistance: Int, result: MutableSet<String>) {
        if (editDistance >= maxDictionaryEditDistance || word.length <= 1) return

        for (i in word.indices) {
            val del = word.substring(0, i) + word.substring(i + 1)
            if (result.add(del)) {
                edits(del, editDistance + 1, result)
            }
        }
    }

    private fun damerauLevenshteinDistance(source: String, target: String, maxDistance: Int): Int {
        val sLen = source.length
        val tLen = target.length

        if (abs(sLen - tLen) > maxDistance) return maxDistance + 1
        if (sLen == 0) return tLen
        if (tLen == 0) return sLen

        val d = Array(sLen + 1) { IntArray(tLen + 1) }

        for (i in 0..sLen) d[i][0] = i
        for (j in 0..tLen) d[0][j] = j

        for (i in 1..sLen) {
            val sc = source[i - 1]
            var minRowCost = Int.MAX_VALUE

            for (j in 1..tLen) {
                val tc = target[j - 1]
                val cost = if (sc == tc) 0 else 1

                var current = min(d[i - 1][j] + 1, min(d[i][j - 1] + 1, d[i - 1][j - 1] + cost))

                // Transposition
                if (i > 1 && j > 1 && sc == target[j - 2] && source[i - 2] == tc) {
                    current = min(current, d[i - 2][j - 2] + 1)
                }

                d[i][j] = current
                minRowCost = min(minRowCost, current)
            }

            if (minRowCost > maxDistance) return maxDistance + 1
        }

        return d[sLen][tLen]
    }
}
