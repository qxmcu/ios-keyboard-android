package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.SymSpell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymSpellTest {

    @Test
    fun testSymSpellLookup() {
        val symSpell = SymSpell(maxDictionaryEditDistance = 2)

        symSpell.createDictionaryEntry("hello", 1000L)
        symSpell.createDictionaryEntry("help", 800L)
        symSpell.createDictionaryEntry("held", 500L)
        symSpell.createDictionaryEntry("world", 900L)

        // Exact match
        val exact = symSpell.lookup("hello", maxEditDistance = 0)
        assertEquals(1, exact.size)
        assertEquals("hello", exact[0].term)
        assertEquals(0, exact[0].distance)

        // Edit distance 1: "hellp" -> "hello", "help"
        val dist1 = symSpell.lookup("hellp", maxEditDistance = 1)
        assertTrue(dist1.isNotEmpty())
        assertEquals("hello", dist1[0].term)

        // Edit distance 2: "hllo" -> "hello" (missing 'e')
        val dist2 = symSpell.lookup("hllo", maxEditDistance = 1)
        assertTrue(dist2.any { it.term == "hello" })
    }

    @Test
    fun testSymSpellFrequencyRanking() {
        val symSpell = SymSpell(maxDictionaryEditDistance = 2)
        symSpell.createDictionaryEntry("the", 50000L)
        symSpell.createDictionaryEntry("they", 20000L)
        symSpell.createDictionaryEntry("them", 15000L)

        val results = symSpell.lookup("th", maxEditDistance = 2)
        assertTrue(results.isNotEmpty())
        assertEquals("the", results[0].term)
    }
}
