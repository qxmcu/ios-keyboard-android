package org.iosclone.keyboard

import org.iosclone.keyboard.layout.KeyType
import org.iosclone.keyboard.layout.KeyboardLayoutFactory
import org.iosclone.keyboard.layout.KeyboardMode
import org.iosclone.keyboard.layout.LanguageLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutFactoryTest {

    @Test
    fun testDefaultLayoutHas4Rows() {
        val layout = KeyboardLayoutFactory.createLayout(
            language = LanguageLayout.QWERTY,
            mode = KeyboardMode.LOWERCASE,
            showNumberRow = false,
            showPeriodKey = false
        )
        assertEquals(4, layout.rows.size)
        // Spacebar is in row 4
        assertTrue(layout.rows[3].any { it.keyType == KeyType.SPACE })
        // Period key should NOT be present in default alpha row 4
        assertTrue(layout.rows[3].none { it.code == 46 && it.label == "." })
    }

    @Test
    fun testDedicatedNumberRowHas5Rows() {
        val layout = KeyboardLayoutFactory.createLayout(
            language = LanguageLayout.QWERTY,
            mode = KeyboardMode.LOWERCASE,
            showNumberRow = true,
            showPeriodKey = false
        )
        assertEquals(5, layout.rows.size)
        // Row 0 is the number row (1 2 3 4 5 6 7 8 9 0)
        assertEquals(10, layout.rows[0].size)
        assertEquals("1", layout.rows[0][0].label)
        assertEquals("0", layout.rows[0][9].label)

        // Row 1 is QWERTY
        assertEquals("q", layout.rows[1][0].label)
    }

    @Test
    fun testDedicatedPeriodKeyInBottomRow() {
        val layout = KeyboardLayoutFactory.createLayout(
            language = LanguageLayout.QWERTY,
            mode = KeyboardMode.LOWERCASE,
            showNumberRow = false,
            showPeriodKey = true
        )
        val bottomRow = layout.rows.last()
        assertTrue(bottomRow.any { it.code == 46 && it.label == "." })
        // Check ordering: space should be before period, period before return
        val spaceIdx = bottomRow.indexOfFirst { it.keyType == KeyType.SPACE }
        val periodIdx = bottomRow.indexOfFirst { it.code == 46 }
        val returnIdx = bottomRow.indexOfFirst { it.keyType == KeyType.RETURN }

        assertTrue(spaceIdx < periodIdx)
        assertTrue(periodIdx < returnIdx)
    }
}
