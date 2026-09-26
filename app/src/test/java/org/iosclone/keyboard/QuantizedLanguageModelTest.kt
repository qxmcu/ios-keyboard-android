package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.QuantizedLanguageModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantizedLanguageModelTest {

    @Test
    fun testNextWordPredictions() {
        val model = QuantizedLanguageModel()

        // "How are" -> "you"
        val afterHowAre = model.predictNextWords(listOf("how", "are"), limit = 3)
        assertTrue("Expected 'you' in predictions for 'how are', got: $afterHowAre", afterHowAre.contains("you"))

        // "thank" -> "you"
        val afterThank = model.predictNextWords(listOf("thank"), limit = 3)
        assertTrue("Expected 'you' in predictions for 'thank', got: $afterThank", afterThank.contains("you"))

        // "on my" -> "way"
        val afterOnMy = model.predictNextWords(listOf("on", "my"), limit = 3)
        assertTrue("Expected 'way' in predictions for 'on my', got: $afterOnMy", afterOnMy.contains("way"))
    }

    @Test
    fun testUserDynamicLearning() {
        val model = QuantizedLanguageModel()

        // Before learning, custom collocation is not predicted
        model.learnBigram("quantum", "computing")
        model.learnBigram("quantum", "computing")
        model.learnBigram("quantum", "computing")

        val predictions = model.predictNextWords(listOf("quantum"), limit = 3)
        assertEquals("computing", predictions.firstOrNull())
    }

    @Test
    fun testSubMillisecondPerformance() {
        val model = QuantizedLanguageModel()
        val start = System.nanoTime()

        for (i in 0 until 1000) {
            model.predictNextWords(listOf("how", "are"), limit = 3)
            model.getContextScore("going", "am")
        }

        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val perOpUs = (elapsedMs * 1000.0) / 2000.0
        assertTrue("Evaluation per operation took $perOpUs µs, must be < 50 µs", perOpUs < 50.0)
    }
}
