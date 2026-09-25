package org.iosclone.keyboard.clipboard

data class ClipboardEntry(
    val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
