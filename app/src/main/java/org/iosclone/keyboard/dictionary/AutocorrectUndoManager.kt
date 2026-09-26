package org.iosclone.keyboard.dictionary

/**
 * Tracks recent autocorrection events to support instant undo on Backspace.
 * When the user types an autocorrected word and immediately presses Backspace,
 * the original verbatim typed text is restored without fighting the user.
 * Rejected corrections are saved permanently in local SQLite so updates never erase user preferences.
 */
class AutocorrectUndoManager(private val userDb: UserDictionaryDb? = null) {

    data class ReplacementRecord(
        val originalTyped: String,
        val autocorrectedTo: String,
        val timestamp: Long
    )

    private var lastReplacement: ReplacementRecord? = null
    private val ignoredWords = mutableSetOf<String>()

    init {
        loadPersistedIgnoredWords()
    }

    private fun loadPersistedIgnoredWords() {
        val db = userDb ?: return
        try {
            val allIgnored = db.getAllIgnoredWords()
            synchronized(ignoredWords) {
                ignoredWords.addAll(allIgnored)
            }
        } catch (_: Exception) {}
    }

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
        if (now - last.timestamp > 3500) {
            lastReplacement = null
            return null
        }

        // Check if cursor is right after the replaced word + space (e.g. "the ")
        val expectedSuffix = last.autocorrectedTo + " "
        val expectedWithoutSpace = last.autocorrectedTo
        if (textBeforeCursor.endsWith(expectedSuffix) || textBeforeCursor.endsWith(expectedWithoutSpace)) {
            // Learn permanently: the user explicitly reverted this autocorrect!
            ignoreWordPermanently(last.originalTyped)
            val record = last
            lastReplacement = null
            return record
        }

        return null
    }

    fun ignoreWordPermanently(word: String) {
        val lower = word.trim().lowercase()
        if (lower.isEmpty()) return
        synchronized(ignoredWords) {
            ignoredWords.add(lower)
        }
        try {
            userDb?.addIgnoredWord(lower)
        } catch (_: Exception) {}
    }

    fun isIgnored(word: String): Boolean {
        val lower = word.trim().lowercase()
        return synchronized(ignoredWords) { ignoredWords.contains(lower) }
    }

    fun clearIgnored(word: String) {
        val lower = word.trim().lowercase()
        synchronized(ignoredWords) {
            ignoredWords.remove(lower)
        }
        try {
            userDb?.removeIgnoredWord(lower)
        } catch (_: Exception) {}
    }

    fun reset() {
        lastReplacement = null
    }
}
