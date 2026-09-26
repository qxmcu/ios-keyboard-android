package org.iosclone.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(parts1.size, parts2.size)

        for (i in 0 until length) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    @Test
    fun testVersionComparison() {
        assertTrue(compareVersions("1.2.5", "1.2.1") > 0)
        assertTrue(compareVersions("1.3.0", "1.2.5") > 0)
        assertTrue(compareVersions("2.0.0", "1.9.9") > 0)
        assertEquals(0, compareVersions("1.2.5", "1.2.5"))
        assertTrue(compareVersions("1.2.1", "1.2.5") < 0)
    }
}
