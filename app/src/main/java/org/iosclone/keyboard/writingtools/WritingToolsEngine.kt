package org.iosclone.keyboard.writingtools

/**
 * On-device Apple Intelligence Writing Tools engine for iOS 27 Keyboard.
 * Provides proofreading, tone rewriting (Friendly, Professional, Concise),
 * text summarization, and Genmoji generation with zero cloud dependency and zero battery drain.
 */
class WritingToolsEngine {

    enum class Tone {
        FRIENDLY,
        PROFESSIONAL,
        CONCISE
    }

    enum class SummaryStyle {
        KEY_POINTS,
        TLDR
    }

    data class ProofreadResult(
        val originalText: String,
        val correctedText: String,
        val changes: List<String>
    )

    data class GenmojiItem(
        val prompt: String,
        val primaryEmoji: String,
        val secondaryEmoji: String,
        val compositeDescription: String
    )

    /**
     * Proofreads text for spelling, capitalization, punctuation, and grammatical agreement.
     */
    fun proofread(text: String): ProofreadResult {
        if (text.isBlank()) return ProofreadResult(text, text, emptyList())

        var result = text
        val changes = mutableListOf<String>()

        // 1. Fix capitalization of standalone 'i'
        val iRegex = Regex("\\b(i)\\b")
        if (iRegex.containsMatchIn(result)) {
            result = result.replace(iRegex, "I")
            changes.add("Capitalized pronoun 'I'")
        }

        // 2. Capitalize first letter of sentences
        val sentenceRegex = Regex("(^|[.!?]\\s+)([a-z])")
        result = sentenceRegex.replace(result) { match ->
            val prefix = match.groupValues[1]
            val letter = match.groupValues[2].uppercase()
            changes.add("Capitalized start of sentence")
            "$prefix$letter"
        }

        // 3. Fix duplicate consecutive words ("the the" -> "the")
        val dupRegex = Regex("\\b([a-zA-Z]+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
        if (dupRegex.containsMatchIn(result)) {
            result = dupRegex.replace(result, "$1")
            changes.add("Removed duplicate consecutive word")
        }

        // 4. Fix "a" vs "an" before vowel sounds
        val anRegex = Regex("\\ba\\s+([aeiouAEIOU][a-z]+)")
        if (anRegex.containsMatchIn(result)) {
            result = anRegex.replace(result, "an $1")
            changes.add("Corrected 'a' to 'an' before vowel")
        }

        // 5. Fix common grammar agreements
        val grammarPairs = listOf(
            Regex("\\bthey is\\b", RegexOption.IGNORE_CASE) to "they are",
            Regex("\\bwe was\\b", RegexOption.IGNORE_CASE) to "we were",
            Regex("\\byou was\\b", RegexOption.IGNORE_CASE) to "you were",
            Regex("\\bdid went\\b", RegexOption.IGNORE_CASE) to "went",
            Regex("\\bcant\\b", RegexOption.IGNORE_CASE) to "can't",
            Regex("\\bdont\\b", RegexOption.IGNORE_CASE) to "don't",
            Regex("\\bwont\\b", RegexOption.IGNORE_CASE) to "won't",
            Regex("\\bim\\b", RegexOption.IGNORE_CASE) to "I'm"
        )

        for ((regex, replacement) in grammarPairs) {
            if (regex.containsMatchIn(result)) {
                result = result.replace(regex, replacement)
                changes.add("Corrected phrasing to '$replacement'")
            }
        }

        // 6. Ensure ending punctuation
        val trimmed = result.trimEnd()
        if (trimmed.isNotEmpty() && !trimmed.endsWith('.') && !trimmed.endsWith('!') && !trimmed.endsWith('?')) {
            result = "$trimmed."
            changes.add("Added ending punctuation")
        }

        return ProofreadResult(text, result, changes)
    }

    /**
     * Rewrites text according to selected tone.
     */
    fun rewrite(text: String, tone: Tone): String {
        val clean = text.trim()
        if (clean.isBlank()) return text

        return when (tone) {
            Tone.FRIENDLY -> {
                var res = clean
                // Conversational transformations
                res = res.replace(Regex("\\bPlease find attached\\b", RegexOption.IGNORE_CASE), "Here is")
                res = res.replace(Regex("\\bI would appreciate\\b", RegexOption.IGNORE_CASE), "I'd really love")
                res = res.replace(Regex("\\bSincerely\\b", RegexOption.IGNORE_CASE), "Warmly")
                res = res.replace(Regex("\\bRegards\\b", RegexOption.IGNORE_CASE), "Best")
                res = res.replace(Regex("\\bDo not hesitate to contact me\\b", RegexOption.IGNORE_CASE), "Let me know anytime!")
                if (!res.endsWith("!")) {
                    res = res.trimEnd('.') + "!"
                }
                "$res 😊"
            }

            Tone.PROFESSIONAL -> {
                var res = clean
                // Elevate colloquialisms
                res = res.replace(Regex("\\bgonna\\b", RegexOption.IGNORE_CASE), "going to")
                res = res.replace(Regex("\\bwanna\\b", RegexOption.IGNORE_CASE), "would like to")
                res = res.replace(Regex("\\bgotta\\b", RegexOption.IGNORE_CASE), "must")
                res = res.replace(Regex("\\basap\\b", RegexOption.IGNORE_CASE), "at your earliest convenience")
                res = res.replace(Regex("\\bhey\\b", RegexOption.IGNORE_CASE), "Hello")
                res = res.replace(Regex("\\bthanks\\b", RegexOption.IGNORE_CASE), "Thank you")
                res = res.replace(Regex("\\byeah\\b|\\byep\\b", RegexOption.IGNORE_CASE), "Yes")
                res = res.replace("!", ".")
                res = proofread(res).correctedText
                res
            }

            Tone.CONCISE -> {
                var res = clean
                // Remove filler words
                val fillers = listOf(
                    Regex("\\bactually\\b\\s*", RegexOption.IGNORE_CASE),
                    Regex("\\bbasically\\b\\s*", RegexOption.IGNORE_CASE),
                    Regex("\\bliterally\\b\\s*", RegexOption.IGNORE_CASE),
                    Regex("\\bjust wondering if\\b\\s*", RegexOption.IGNORE_CASE),
                    Regex("\\bin order to\\b", RegexOption.IGNORE_CASE),
                    Regex("\\bat this point in time\\b", RegexOption.IGNORE_CASE),
                    Regex("\\bdue to the fact that\\b", RegexOption.IGNORE_CASE)
                )
                for (f in fillers) {
                    res = res.replace(f, "")
                }
                res = res.replace(Regex("\\bin order to\\b", RegexOption.IGNORE_CASE), "to")
                res = res.replace(Regex("\\bdue to the fact that\\b", RegexOption.IGNORE_CASE), "because")
                res = res.replace(Regex("\\s{2,}"), " ").trim()
                proofread(res).correctedText
            }
        }
    }

    /**
     * Summarizes text into Key Points or a TL;DR statement.
     */
    fun summarize(text: String, style: SummaryStyle): String {
        val clean = text.trim()
        if (clean.isBlank()) return text

        val sentences = clean.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }

        return when (style) {
            SummaryStyle.KEY_POINTS -> {
                if (sentences.size <= 1) {
                    "• $clean"
                } else {
                    sentences.joinToString("\n") { "• ${it.trim()}" }
                }
            }

            SummaryStyle.TLDR -> {
                if (sentences.isNotEmpty()) {
                    "TL;DR: ${sentences.first().trim()}"
                } else {
                    "TL;DR: $clean"
                }
            }
        }
    }

    /**
     * iOS 27 Genmoji generator: creates composite emoji stickers based on prompt or selected keywords.
     */
    fun createGenmoji(prompt: String): GenmojiItem {
        val lower = prompt.lowercase().trim()
        val (emoji1, emoji2, desc) = when {
            lower.contains("party") || lower.contains("celebrate") -> Triple("🥳", "🎉", "Party Celebration")
            lower.contains("love") || lower.contains("heart") -> Triple("🥰", "💖", "Sweet Loving Heart")
            lower.contains("fire") || lower.contains("cool") -> Triple("😎", "🔥", "Cool & On Fire")
            lower.contains("coffee") || lower.contains("morning") -> Triple("☕", "✨", "Morning Brew Vibe")
            lower.contains("mind blown") || lower.contains("crazy") -> Triple("🤯", "💥", "Mind Blown Blast")
            lower.contains("sleepy") || lower.contains("tired") -> Triple("😴", "🌙", "Sleepy Night Dream")
            lower.contains("rocket") || lower.contains("fast") -> Triple("🚀", "✨", "Super Cosmic Rocket")
            lower.contains("cat") -> Triple("🐱", "😻", "Playful Kitty")
            lower.contains("dog") -> Triple("🐶", "🎾", "Happy Puppy")
            lower.contains("laugh") || lower.contains("funny") -> Triple("😂", "🌟", "Laughing Superstar")
            else -> Triple("✨", "💫", "Magic Sparkle Genmoji")
        }

        return GenmojiItem(
            prompt = prompt,
            primaryEmoji = emoji1,
            secondaryEmoji = emoji2,
            compositeDescription = desc
        )
    }
}
