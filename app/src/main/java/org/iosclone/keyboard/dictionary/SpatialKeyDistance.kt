package org.iosclone.keyboard.dictionary

import kotlin.math.hypot
import kotlin.math.min

/**
 * Calculates spatial Euclidean distance between physical keys on a mobile QWERTY keyboard.
 * Used to score typo probability: an accidental tap of an adjacent key (e.g. 'h' for 'g')
 * receives a fractional penalty rather than a full edit distance penalty.
 */
object SpatialKeyDistance {

    // Normalized (x, y) coordinates for standard iOS QWERTY layout
    private val keyCoords = mapOf(
        'q' to Pair(0.5f, 0.5f), 'w' to Pair(1.5f, 0.5f), 'e' to Pair(2.5f, 0.5f), 'r' to Pair(3.5f, 0.5f),
        't' to Pair(4.5f, 0.5f), 'y' to Pair(5.5f, 0.5f), 'u' to Pair(6.5f, 0.5f), 'i' to Pair(7.5f, 0.5f),
        'o' to Pair(8.5f, 0.5f), 'p' to Pair(9.5f, 0.5f),

        'a' to Pair(1.0f, 1.5f), 's' to Pair(2.0f, 1.5f), 'd' to Pair(3.0f, 1.5f), 'f' to Pair(4.0f, 1.5f),
        'g' to Pair(5.0f, 1.5f), 'h' to Pair(6.0f, 1.5f), 'j' to Pair(7.0f, 1.5f), 'k' to Pair(8.0f, 1.5f),
        'l' to Pair(9.0f, 1.5f),

        'z' to Pair(2.0f, 2.5f), 'x' to Pair(3.0f, 2.5f), 'c' to Pair(4.0f, 2.5f), 'v' to Pair(5.0f, 2.5f),
        'b' to Pair(6.0f, 2.5f), 'n' to Pair(7.0f, 2.5f), 'm' to Pair(8.0f, 2.5f)
    )

    /**
     * Returns substitution penalty between 0.0f (identical) and 1.0f (distant).
     * Adjacent keys on keyboard have penalty ~0.35f to 0.45f.
     */
    fun substitutionCost(c1: Char, c2: Char): Float {
        val ch1 = c1.lowercaseChar()
        val ch2 = c2.lowercaseChar()
        if (ch1 == ch2) return 0.0f

        val coord1 = keyCoords[ch1]
        val coord2 = keyCoords[ch2]
        if (coord1 == null || coord2 == null) return 1.0f

        val dist = hypot(coord1.first - coord2.first, coord1.second - coord2.second)
        // If keys are immediate horizontal or diagonal neighbors (dist <= 1.42)
        return when {
            dist <= 1.05f -> 0.35f // Immediate horizontal neighbor (e.g. f-g, h-j)
            dist <= 1.45f -> 0.50f // Immediate diagonal neighbor (e.g. g-y, f-r, d-x)
            dist <= 2.05f -> 0.75f // Near neighbor
            else -> 1.0f           // Distant keys
        }
    }

    /**
     * Damerau-Levenshtein distance with spatial key penalties and transposition support.
     * E.g. "teh" vs "the" = 0.8f (single transposition).
     * "giod" vs "good" = 0.4f (adjacent 'i'-'o' substitution).
     */
    fun spatialDistance(typed: String, target: String): Float {
        val s1 = typed.lowercase()
        val s2 = target.lowercase()
        val n = s1.length
        val m = s2.length

        if (n == 0) return m.toFloat()
        if (m == 0) return n.toFloat()

        val dp = Array(n + 1) { FloatArray(m + 1) }

        for (i in 0..n) dp[i][0] = i.toFloat()
        for (j in 0..m) dp[0][j] = j.toFloat()

        for (i in 1..n) {
            val ch1 = s1[i - 1]
            for (j in 1..m) {
                val ch2 = s2[j - 1]
                val subCost = substitutionCost(ch1, ch2)

                val deleteCost = dp[i - 1][j] + 1.0f
                val insertCost = dp[i][j - 1] + 1.0f
                val replaceCost = dp[i - 1][j - 1] + subCost

                var best = min(deleteCost, min(insertCost, replaceCost))

                // Transposition check (Damerau)
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    best = min(best, dp[i - 2][j - 2] + 0.85f)
                }

                dp[i][j] = best
            }
        }

        return dp[n][m]
    }
}
