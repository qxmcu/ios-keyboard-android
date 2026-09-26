package org.iosclone.keyboard

import org.iosclone.keyboard.writingtools.WritingToolsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingToolsEngineTest {

    private val engine = WritingToolsEngine()

    @Test
    fun testProofreading() {
        val input = "i went to the store. they is going too"
        val result = engine.proofread(input)

        // Verifies standalone 'I' capitalization and grammar correction
        assertTrue("Expected 'I', got: ${result.correctedText}", result.correctedText.startsWith("I went"))
        assertTrue("Expected 'they are', got: ${result.correctedText}", result.correctedText.contains("they are", ignoreCase = true))
        assertTrue("Expected ending punctuation", result.correctedText.endsWith('.'))
    }

    @Test
    fun testToneRewriting() {
        val input = "Please find attached the file. Let me know."
        val friendly = engine.rewrite(input, WritingToolsEngine.Tone.FRIENDLY)
        assertTrue("Friendly rewrite should have warm tone: $friendly", friendly.contains("😊") || friendly.contains("Here is"))

        val informal = "gonna do this asap thanks"
        val professional = engine.rewrite(informal, WritingToolsEngine.Tone.PROFESSIONAL)
        assertTrue("Professional rewrite should elevate words: $professional", professional.contains("going to", ignoreCase = true) || professional.contains("thank you", ignoreCase = true))

        val verbose = "I am basically just wondering if in order to proceed we can do this."
        val concise = engine.rewrite(verbose, WritingToolsEngine.Tone.CONCISE)
        assertTrue("Concise should remove filler words: $concise", !concise.contains("basically") && !concise.contains("just wondering if"))
    }

    @Test
    fun testGenmojiGeneration() {
        val genmoji = engine.createGenmoji("party birthday celebration")
        assertEquals("🥳", genmoji.primaryEmoji)
        assertEquals("🎉", genmoji.secondaryEmoji)
    }
}
