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

    @Test
    fun testPhysicalTouchPointsDistance() {
        val keyMatrix = mapOf(
            'v' to org.iosclone.keyboard.dictionary.KeyCenter(100f, 200f),
            'b' to org.iosclone.keyboard.dictionary.KeyCenter(150f, 200f),
            'p' to org.iosclone.keyboard.dictionary.KeyCenter(400f, 50f)
        )

        // User typed 'v' but physical touch was at (135f, 200f), very close to 'b' (150f)
        val touches = listOf(
            org.iosclone.keyboard.dictionary.TouchPoint('v', 135f, 200f)
        )

        val costToB = SpatialKeyDistance.spatialDistance("v", "b", touches, keyMatrix)
        val costToP = SpatialKeyDistance.spatialDistance("v", "p", touches, keyMatrix)

        assertTrue("Cost to physically close key 'b' should be lower than distant 'p'", costToB < costToP)
        assertTrue("Cost to 'b' should be <= 0.45", costToB <= 0.45f)
    }
}
