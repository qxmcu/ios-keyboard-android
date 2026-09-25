package org.iosclone.keyboard.emoji

data class EmojiItem(
    val unicode: String,
    val name: String,
    val category: EmojiCategory,
    val tags: List<String> = emptyList(),
    val supportsSkinTone: Boolean = false
) {
    fun matchesQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        if (name.lowercase().contains(q)) return true
        return tags.any { it.lowercase().contains(q) }
    }
}
