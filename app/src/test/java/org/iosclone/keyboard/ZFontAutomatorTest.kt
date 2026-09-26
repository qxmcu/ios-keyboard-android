package org.iosclone.keyboard

import org.iosclone.keyboard.emoji.ZFontAutomator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ZFontAutomatorTest {

    @Test
    fun testZFontPackageAndFileName() {
        assertEquals("com.mgng.zfont3", ZFontAutomator.ZFONT_PACKAGE)
        assertEquals("iOS_26.4_AppleColorEmoji.ttf", ZFontAutomator.FONT_FILE_NAME)
    }
}
