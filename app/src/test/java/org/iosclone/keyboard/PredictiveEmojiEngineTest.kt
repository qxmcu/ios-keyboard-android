package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.PredictiveEmojiEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveEmojiEngineTest {

    @Test
    fun testEmojiAssociations() {
        val fireEmojis = PredictiveEmojiEngine.getSuggestedEmojis("fire")
        assertNotNull(fireEmojis)
        assertTrue(fireEmojis!!.contains("🔥"))

        val pizzaEmojis = PredictiveEmojiEngine.getSuggestedEmojis("pizza")
        assertNotNull(pizzaEmojis)
        assertTrue(pizzaEmojis!!.contains("🍕"))

        val loveEmojis = PredictiveEmojiEngine.getSuggestedEmojis("love")
        assertNotNull(loveEmojis)
        assertTrue(loveEmojis!!.contains("❤️"))

        val coffeeEmojis = PredictiveEmojiEngine.getSuggestedEmojis("coffee")
        assertNotNull(coffeeEmojis)
        assertTrue(coffeeEmojis!!.contains("☕"))
    }
}
