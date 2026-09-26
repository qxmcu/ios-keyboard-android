package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.SpatialKeyDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialKeyDistanceTest {

    @Test
    fun testAdjacentKeySubstitutionCost() {
        // 'g' and 'h' are horizontal neighbors on QWERTY
        val adjacentCost = SpatialKeyDistance.substitutionCost('g', 'h')
        assertTrue("Adjacent keys should have cost <= 0.45", adjacentCost <= 0.45f)

        // 'a' and 'p' are distant keys on opposite sides of keyboard
        val distantCost = SpatialKeyDistance.substitutionCost('a', 'p')
        assertEquals(1.0f, distantCost, 0.01f)

        // Identical keys cost 0
        assertEquals(0.0f, SpatialKeyDistance.substitutionCost('e', 'e'), 0.001f)
    }

    @Test
    fun testSpatialDamerauDistance() {
        // Transposition "teh" vs "the" should be penalized less than 2 operations
        val transpositionDist = SpatialKeyDistance.spatialDistance("teh", "the")
        assertTrue("Transposition should be scored <= 1.0", transpositionDist <= 1.0f)

        // Typo on adjacent key: "giod" vs "good" ('i' is next to 'o')
        val adjacentTypoDist = SpatialKeyDistance.spatialDistance("giod", "good")
        assertTrue("Adjacent typo should be scored <= 0.6", adjacentTypoDist <= 0.6f)
    }
}
