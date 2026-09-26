package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.ContractionFixer
import org.junit.Assert.assertEquals
import org.junit.Test

class ContractionFixerTest {

    @Test
    fun testContractionsWithoutApostrophe() {
        assertEquals("don't", ContractionFixer.getFix("dont"))
        assertEquals("can't", ContractionFixer.getFix("cant"))
        assertEquals("won't", ContractionFixer.getFix("wont"))
        assertEquals("I'm", ContractionFixer.getFix("im"))
        assertEquals("you're", ContractionFixer.getFix("youre"))
        assertEquals("they're", ContractionFixer.getFix("theyre"))
        assertEquals("that's", ContractionFixer.getFix("thats"))
        assertEquals("I'll", ContractionFixer.getFix("ill"))
    }

    @Test
    fun testCommonPhoneticMisspellings() {
        assertEquals("definitely", ContractionFixer.getFix("definately"))
        assertEquals("receive", ContractionFixer.getFix("recieve"))
        assertEquals("separate", ContractionFixer.getFix("seperate"))
        assertEquals("the", ContractionFixer.getFix("teh"))
        assertEquals("this", ContractionFixer.getFix("thsi"))
        assertEquals("tomorrow", ContractionFixer.getFix("tomorow"))
    }
}
