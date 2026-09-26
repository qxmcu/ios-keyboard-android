package org.iosclone.keyboard.dictionary

/**
 * Tracks recent autocorrection events to support instant undo on Backspace.
 * When the user types an autocorrected word and immediately presses Backspace,
 * the original verbatim typed text is restored without fighting the user.
 */
class AutocorrectUndoManager {

    data class ReplacementRecord(
        val originalTyped: String,
        val autocorrectedTo: String,
        val timestamp: Long
    )

    private var lastReplacement: ReplacementRecord? = null
    private val temporarilyIgnoredWords = mutableSetOf<String>()

    fun recordReplacement(original: String, replaced: String) {
        lastReplacement = ReplacementRecord(
            originalTyped = original,
            autocorrectedTo = replaced,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Checks if a backspace press should trigger an undo of the last autocorrection.
     */
    fun shouldUndoOnDelete(textBeforeCursor: String): ReplacementRecord? {
        val last = lastReplacement ?: return null
        val now = System.currentTimeMillis()
        if (now - last.timestamp > 3000) {
            lastReplacement = null
            return null
        }

        // Check if cursor is right after the replaced word + space (e.g. "the ")
        val expectedSuffix = last.autocorrectedTo + " "
        val expectedWithoutSpace = last.autocorrectedTo
        if (textBeforeCursor.endsWith(expectedSuffix) || textBeforeCursor.endsWith(expectedWithoutSpace)) {
            // Temporarily ignore this word so typing space doesn't immediately re-correct it
            temporarilyIgnoredWords.add(last.originalTyped.lowercase())
            val record = last
            lastReplacement = null
            return record
        }

        return null
    }

    fun isIgnored(word: String): Boolean {
        return temporarilyIgnoredWords.contains(word.lowercase())
    }

    fun clearIgnored(word: String) {
        temporarilyIgnoredWords.remove(word.lowercase())
    }

    fun reset() {
        lastReplacement = null
    }
}
