package org.iosclone.keyboard.dictionary

data class AutocorrectResult(
    val centerCandidate: String,
    val leftCandidate: String,
    val rightCandidate: String,
    val isExactMatch: Boolean,
    val rawTypedWord: String
) {
    companion object {
        val Empty = AutocorrectResult("", "", "", false, "")
    }
}
