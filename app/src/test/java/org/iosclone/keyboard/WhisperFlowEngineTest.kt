package org.iosclone.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperFlowEngineTest {

    @Test
    fun testSpokenPunctuationFormatting() {
        val spokenPunctuation = mapOf(
            "comma" to ",",
            "period" to ".",
            "question mark" to "?",
            "exclamation mark" to "!"
        )

        fun format(raw: String): String {
            var text = raw.trim()
            for ((spoken, sym) in spokenPunctuation) {
                text = text.replace(Regex("(?i)\\b$spoken\\b"), sym)
            }
            text = text.replace(Regex("\\s+([.,!?:;])"), "$1")
            return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val formatted = format("hello comma how are you question mark")
        assertEquals("Hello, how are you?", formatted)
    }

    @Test
    fun testSpokenEmojisFormatting() {
        val spokenEmojis = mapOf(
            "heart emoji" to "❤️",
            "fire emoji" to "🔥"
        )

        fun format(raw: String): String {
            var text = raw.trim()
            for ((spoken, emoji) in spokenEmojis) {
                text = text.replace(Regex("(?i)\\b$spoken\\b"), emoji)
            }
            return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val formatted = format("great job fire emoji")
        assertEquals("Great job 🔥", formatted)
    }
}
