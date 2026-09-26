package org.iosclone.keyboard.dictionary

/**
 * High-speed inverted index for predictive emoji suggestions in iOS QuickType.
 * When the user types an associative word (e.g. "fire", "love", "pizza"),
 * matching emojis are suggested directly in the predictive bar.
 */
object PredictiveEmojiEngine {

    private val emojiMap = mapOf(
        // Emotions & Faces
        "happy" to listOf("😊", "😄", "😁"),
        "smile" to listOf("🙂", "😊", "😀"),
        "love" to listOf("❤️", "😍", "🥰", "💕"),
        "heart" to listOf("❤️", "💖", "💓"),
        "sad" to listOf("😢", "😭", "😞"),
        "cry" to listOf("😭", "😢"),
        "laugh" to listOf("😂", "🤣", "😆"),
        "lol" to listOf("😂", "🤣"),
        "haha" to listOf("😂", "😄"),
        "cool" to listOf("😎", "🤙"),
        "fire" to listOf("🔥"),
        "lit" to listOf("🔥"),
        "kiss" to listOf("😘", "💋"),
        "angry" to listOf("😡", "🤬", "😠"),
        "mad" to listOf("😡", "😤"),
        "sleep" to listOf("😴", "💤"),
        "tired" to listOf("🥱", "😴"),
        "think" to listOf("🤔"),
        "wink" to listOf("😉"),
        "surprised" to listOf("😮", "😲", "🤯"),
        "wow" to listOf("🤩", "😮"),
        "scared" to listOf("😱", "😨"),
        "sick" to listOf("🤢", "🤒"),

        // Gestures & Social
        "yes" to listOf("👍", "✅"),
        "ok" to listOf("👌", "👍"),
        "good" to listOf("👍", "✨"),
        "thumbsup" to listOf("👍"),
        "clap" to listOf("👏"),
        "wave" to listOf("👋"),
        "bye" to listOf("👋"),
        "hi" to listOf("👋"),
        "hello" to listOf("👋"),
        "pray" to listOf("🙏"),
        "thanks" to listOf("🙏", "❤️"),
        "party" to listOf("🎉", "🥳", "🎊"),
        "congrats" to listOf("🎉", "🥳"),
        "cheers" to listOf("🥂", "🍻"),
        "gift" to listOf("🎁"),

        // Food & Drink
        "pizza" to listOf("🍕"),
        "burger" to listOf("🍔"),
        "coffee" to listOf("☕"),
        "tea" to listOf("🍵", "🫖"),
        "beer" to listOf("🍺", "🍻"),
        "wine" to listOf("🍷", "🍾"),
        "cake" to listOf("🎂", "🍰"),
        "birthday" to listOf("🎂", "🎉"),
        "taco" to listOf("🌮"),
        "apple" to listOf("🍎", "🍏"),
        "icecream" to listOf("🍦", "🍨"),
        "cookie" to listOf("🍪"),
        "food" to listOf("🍔", "🍕", "🍜"),

        // Nature & Animals
        "dog" to listOf("🐶", "🐕"),
        "cat" to listOf("🐱", "🐈"),
        "sun" to listOf("☀️"),
        "moon" to listOf("🌙", "🌕"),
        "star" to listOf("⭐", "✨"),
        "rain" to listOf("🌧️", "☔"),
        "snow" to listOf("❄️", "☃️"),
        "flower" to listOf("🌸", "🌹", "🌺"),
        "tree" to listOf("🌲", "🌳"),

        // Travel, Places & Objects
        "car" to listOf("🚗", "🏎️"),
        "plane" to listOf("✈️"),
        "flight" to listOf("✈️", "🛫"),
        "train" to listOf("🚆"),
        "money" to listOf("💰", "💵"),
        "cash" to listOf("💵", "💰"),
        "house" to listOf("🏠"),
        "home" to listOf("🏡", "🏠"),
        "phone" to listOf("📱"),
        "music" to listOf("🎵", "🎶"),
        "game" to listOf("🎮"),
        "football" to listOf("⚽", "🏈"),
        "basketball" to listOf("🏀"),
        "work" to listOf("💼", "💻"),
        "rocket" to listOf("🚀"),
        "magic" to listOf("✨", "🪄")
    )

    /**
     * Returns matching emojis for a typed word, or null if no strong association exists.
     */
    fun getSuggestedEmojis(word: String): List<String>? {
        val clean = word.lowercase().trim().replace(Regex("[^a-z]"), "")
        return emojiMap[clean]
    }
}
