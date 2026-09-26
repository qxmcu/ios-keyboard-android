package org.iosclone.keyboard.dictionary

/**
 * High-precision dictionary of English contractions and irregular high-frequency phonetic typos.
 * Delegates to TypoCorrectionCorpus for 600+ instant deterministic autocorrection fixes.
 */
object ContractionFixer {

    fun getFix(word: String): String? {
        return TypoCorrectionCorpus.findCorrection(word)
    }
}
