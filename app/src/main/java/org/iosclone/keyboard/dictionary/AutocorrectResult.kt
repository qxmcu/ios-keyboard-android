package org.iosclone.keyboard.dictionary

data class AutocorrectResult(
    val centerCandidate: String,
    val leftCandidate: String,
    val rightCandidate: String,
    val isExactMatch: Boolean,
    val rawTypedWord: String,
    val isAutocorrectCandidate: Boolean = false,
    val suggestedEmoji: String? = null,
    val inlinePrediction: String? = null,
    val shortcutReplacement: String? = null
) {
    companion object {
        val Empty = AutocorrectResult(
            centerCandidate = "",
            leftCandidate = "",
            rightCandidate = "",
            isExactMatch = false,
            rawTypedWord = "",
            isAutocorrectCandidate = false,
            suggestedEmoji = null,
            inlinePrediction = null,
            shortcutReplacement = null
        )
    }
}
