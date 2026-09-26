package org.iosclone.keyboard.dictionary

import kotlin.math.min

class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isWord: Boolean = false
    var frequency: Int = 0
}

class Trie {
    val root = TrieNode()

    fun insert(word: String, frequency: Int) {
        if (word.isBlank()) return
        var current = root
        val lower = word.lowercase()
        for (ch in lower) {
            current = current.children.getOrPut(ch) { TrieNode() }
        }
        current.isWord = true
        current.frequency = maxOf(current.frequency, frequency)
    }

    fun contains(word: String): Boolean {
        var current = root
        for (ch in word.lowercase()) {
            current = current.children[ch] ?: return false
        }
        return current.isWord
    }

    /**
     * Retrieves words starting with prefix, sorted by descending frequency.
     */
    fun findPrefixSuggestions(prefix: String, limit: Int = 5): List<Pair<String, Int>> {
        val results = mutableListOf<Pair<String, Int>>()
        var current = root
        val lower = prefix.lowercase()

        for (ch in lower) {
            current = current.children[ch] ?: return emptyList()
        }

        collectWords(current, StringBuilder(lower), results)
        return results.sortedByDescending { it.second }.take(limit)
    }

    private fun collectWords(node: TrieNode, sb: StringBuilder, results: MutableList<Pair<String, Int>>) {
        if (node.isWord) {
            results.add(Pair(sb.toString(), node.frequency))
        }
        for ((ch, child) in node.children) {
            sb.append(ch)
            collectWords(child, sb, results)
            sb.deleteCharAt(sb.length - 1)
        }
    }

    /**
     * Fuzzy search for autocorrection (SymSpell-style / Levenshtein search in Trie).
     * Finds candidates within maxCost edits (1 or 2).
     */
    fun searchFuzzy(word: String, maxCost: Int = 2, limit: Int = 5): List<Pair<String, Int>> {
        val target = word.lowercase()
        val currentRow = IntArray(target.length + 1) { it }
        val results = mutableListOf<Pair<String, Int>>()

        for ((ch, child) in root.children) {
            searchRecursive(child, ch, target, currentRow, results, maxCost, StringBuilder().append(ch))
        }

        return results.sortedWith(
            compareByDescending<Pair<String, Int>> { it.second }
                .thenBy { levenshteinDistance(it.first, target) }
        ).take(limit)
    }

    private fun searchRecursive(
        node: TrieNode,
        ch: Char,
        target: String,
        previousRow: IntArray,
        results: MutableList<Pair<String, Int>>,
        maxCost: Int,
        currentWord: StringBuilder
    ) {
        val columns = target.length + 1
        val currentRow = IntArray(columns)
        currentRow[0] = previousRow[0] + 1

        var minRowValue = currentRow[0]

        for (col in 1 until columns) {
            val insertCost = currentRow[col - 1] + 1
            val deleteCost = previousRow[col] + 1
            val replaceCost = if (target[col - 1] == ch) previousRow[col - 1] else previousRow[col - 1] + 1

            currentRow[col] = min(insertCost, min(deleteCost, replaceCost))
            if (currentRow[col] < minRowValue) {
                minRowValue = currentRow[col]
            }
        }

        if (currentRow[columns - 1] <= maxCost && node.isWord) {
            results.add(Pair(currentWord.toString(), node.frequency))
            if (results.size >= 40) return
        }

        if (minRowValue <= maxCost && results.size < 40) {
            for ((nextCh, child) in node.children) {
                currentWord.append(nextCh)
                searchRecursive(child, nextCh, target, currentRow, results, maxCost, currentWord)
                currentWord.deleteCharAt(currentWord.length - 1)
                if (results.size >= 40) break
            }
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1]
                else 1 + min(dp[i - 1][j], min(dp[i][j - 1], dp[i - 1][j - 1]))
            }
        }
        return dp[s1.length][s2.length]
    }
}
