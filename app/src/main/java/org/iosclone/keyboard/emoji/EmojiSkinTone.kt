package org.iosclone.keyboard.emoji

enum class EmojiSkinTone(val modifier: String, val displayName: String) {
    DEFAULT("", "Default"),
    LIGHT("\uD83C\uDFFB", "Light"),
    MEDIUM_LIGHT("\uD83C\uDFFC", "Medium-Light"),
    MEDIUM("\uD83C\uDFFD", "Medium"),
    MEDIUM_DARK("\uD83C\uDFFE", "Medium-Dark"),
    DARK("\uD83C\uDFFF", "Dark");

    fun applyTo(baseEmoji: String): String {
        if (this == DEFAULT || baseEmoji.isEmpty()) return baseEmoji
        // Check if already modified
        val cleanBase = baseEmoji
            .replace("\uD83C\uDFFB", "")
            .replace("\uD83C\uDFFC", "")
            .replace("\uD83C\uDFFD", "")
            .replace("\uD83C\uDFFE", "")
            .replace("\uD83C\uDFFF", "")

        return cleanBase + modifier
    }
}
