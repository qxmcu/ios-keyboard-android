package org.iosclone.keyboard

import org.iosclone.keyboard.writingtools.SmallLanguageModelEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmallLanguageModelEngineTest {

    private val slm = SmallLanguageModelEngine()

    @Test
    fun testGenerativeRewriteFriendly() {
        val result = slm.generate("I am writing to inform you that we need this soon.", SmallLanguageModelEngine.Task.REWRITE_FRIENDLY)
        assertTrue(result.outputText.isNotEmpty())
        assertTrue(result.tokenCount > 0)
    }

    @Test
    fun testGenerativeRewriteProfessional() {
        val result = slm.generate("gonna talk about this asap thanks", SmallLanguageModelEngine.Task.REWRITE_PROFESSIONAL)
        assertTrue(result.outputText.contains("going to", ignoreCase = true) || result.outputText.contains("thank", ignoreCase = true))
    }

    @Test
    fun testGenerativeCompose() {
        val sickLeave = slm.generate("sick leave", SmallLanguageModelEngine.Task.COMPOSE)
        assertTrue(sickLeave.outputText.contains("sick leave", ignoreCase = true) || sickLeave.outputText.contains("not feeling well", ignoreCase = true))

        val apology = slm.generate("apology for late reply", SmallLanguageModelEngine.Task.COMPOSE)
        assertTrue(apology.outputText.contains("apologize", ignoreCase = true) || apology.outputText.contains("patience", ignoreCase = true))
    }

    @Test
    fun testGenerativeSummaryAndList() {
        val text = "First point. Second point. Third point."
        val keyPoints = slm.generate(text, SmallLanguageModelEngine.Task.SUMMARIZE_KEY_POINTS)
        assertTrue(keyPoints.outputText.contains("•"))

        val list = slm.generate("apples, bananas, oranges", SmallLanguageModelEngine.Task.FORMAT_LIST)
        assertTrue(list.outputText.contains("•"))
    }
}
