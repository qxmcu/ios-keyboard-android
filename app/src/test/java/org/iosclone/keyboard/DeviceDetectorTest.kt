package org.iosclone.keyboard

import org.iosclone.keyboard.emoji.DeviceDetector
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceDetectorTest {

    @Test
    fun testBrandDisplayNameNotEmpty() {
        val brand = DeviceDetector.getBrandDisplayName()
        assertNotNull(brand)
        assertTrue(brand.isNotBlank())
    }

    @Test
    fun testShortBrandNameNotEmpty() {
        val shortBrand = DeviceDetector.getShortBrandName()
        assertNotNull(shortBrand)
        assertTrue(shortBrand.isNotBlank())
    }

    @Test
    fun testOEMGuideDetailsProvidesInstructions() {
        val guide = DeviceDetector.getOEMGuideDetails()
        assertNotNull(guide)
        assertTrue(guide.contains("zFont 3") || guide.contains("iOS emojis"))
    }
}
