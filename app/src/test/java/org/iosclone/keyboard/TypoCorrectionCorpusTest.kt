package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.TypoCorrectionCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypoCorrectionCorpusTest {

    @Test
    fun testCorpusSize() {
        assertTrue("Corpus should contain over 300 typo rules", TypoCorrectionCorpus.typoMap.size > 300)
    }

    @Test
    fun testDeterministicTypoFixes() {
        assertEquals("the", TypoCorrectionCorpus.findCorrection("teh"))
        assertEquals("definitely", TypoCorrectionCorpus.findCorrection("definately"))
        assertEquals("because", TypoCorrectionCorpus.findCorrection("becuase"))
        assertEquals("receive", TypoCorrectionCorpus.findCorrection("recieve"))
        assertEquals("separate", TypoCorrectionCorpus.findCorrection("seperate"))
        assertEquals("accommodate", TypoCorrectionCorpus.findCorrection("accomodate"))
        assertEquals("tomorrow", TypoCorrectionCorpus.findCorrection("tomorow"))
        assertEquals("government", TypoCorrectionCorpus.findCorrection("goverment"))
    }

    @Test
    fun testContractionCorrections() {
        assertEquals("don't", TypoCorrectionCorpus.findCorrection("dont"))
        assertEquals("can't", TypoCorrectionCorpus.findCorrection("cant"))
        assertEquals("won't", TypoCorrectionCorpus.findCorrection("wont"))
        assertEquals("I'm", TypoCorrectionCorpus.findCorrection("im"))
        assertEquals("they're", TypoCorrectionCorpus.findCorrection("theyre"))
        assertEquals("you're", TypoCorrectionCorpus.findCorrection("youre"))
    }

    @Test
    fun testCapitalizationPreservation() {
        assertEquals("The", TypoCorrectionCorpus.findCorrection("Teh"))
        assertEquals("Definitely", TypoCorrectionCorpus.findCorrection("Definately"))
        assertEquals("BECAUSE", TypoCorrectionCorpus.findCorrection("BECUASE"))
    }
}
