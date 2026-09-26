package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.Trie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrieFuzzySearchTest {

    @Test
    fun testTrieFuzzySearchBounded() {
        val trie = Trie()
        trie.insert("apple", 100)
        trie.insert("apply", 90)
        trie.insert("banana", 80)
        trie.insert("application", 70)

        // Exact
        assertTrue(trie.contains("apple"))
        assertTrue(trie.contains("banana"))

        // Fuzzy match within distance 1
        val results = trie.searchFuzzy("appla", maxCost = 1, limit = 5)
        assertTrue(results.isNotEmpty())
        assertEquals("apple", results[0].first)

        // Fuzzy match within distance 2
        val results2 = trie.searchFuzzy("aple", maxCost = 2, limit = 5)
        assertTrue(results2.any { it.first == "apple" })

        // Safety on empty or huge input
        assertTrue(trie.searchFuzzy("").isEmpty())
        assertTrue(trie.searchFuzzy("a".repeat(40)).isEmpty())
    }
}
