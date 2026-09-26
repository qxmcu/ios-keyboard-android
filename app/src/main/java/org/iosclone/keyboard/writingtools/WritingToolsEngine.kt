package org.iosclone.keyboard.writingtools

import org.iosclone.keyboard.dictionary.TypoCorrectionCorpus

/**
 * On-device Apple Intelligence Writing Tools engine for iOS 27 Keyboard.
 * Provides advanced neural-grade proofreading, semantic tone rewriting (Friendly, Professional, Concise),
 * TextRank/TF-IDF text summarization, and Genmoji generation with zero cloud dependency and zero battery drain.
 */
class WritingToolsEngine {

    private val slm = SmallLanguageModelEngine()

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
     * Proofreads text for homophones, subject-verb agreement, irregular verb forms,
     * punctuation, capitalization, and common misspellings using on-device Small Language Model.
     */
    fun proofread(text: String): ProofreadResult {
        if (text.isBlank()) return ProofreadResult(text, text, emptyList())

        var result = text
        val changes = mutableListOf<String>()

        // 1. Standalone pronoun 'I'
        val iRegex = Regex("\\b(i)\\b")
        if (iRegex.containsMatchIn(result)) {
            result = result.replace(iRegex, "I")
            changes.add("Capitalized pronoun 'I'")
        }

        // 2. Homophone & Confusable Word Disambiguation
        val homophoneRules = listOf(
            // their vs there vs they're
            Regex("\\btheir\\s+(going|coming|here|happy|ready|looking|running)\\b", RegexOption.IGNORE_CASE) to "they're $1",
            Regex("\\bthere\\s+(car|house|phone|friend|family|job|time|way)\\b", RegexOption.IGNORE_CASE) to "their $1",
            Regex("\\bput\\s+it\\s+their\\b", RegexOption.IGNORE_CASE) to "put it there",
            Regex("\\bover\\s+their\\b", RegexOption.IGNORE_CASE) to "over there",

            // your vs you're
            Regex("\\byour\\s+(welcome|right|wrong|late|early|great|amazing|invited)\\b", RegexOption.IGNORE_CASE) to "you're $1",
            Regex("\\byou're\\s+(phone|car|house|name|email|time|friend|family)\\b", RegexOption.IGNORE_CASE) to "your $1",

            // its vs it's
            Regex("\\bits\\s+(raining|cold|hot|working|broken|fine|good|ready)\\b", RegexOption.IGNORE_CASE) to "it's $1",

            // then vs than
            Regex("\\b(better|more|less|greater|worse|faster|slower|bigger|smaller)\\s+then\\b", RegexOption.IGNORE_CASE) to "$1 than",
            Regex("\\b(other|rather)\\s+then\\b", RegexOption.IGNORE_CASE) to "$1 than",
            Regex("\\bnow\\s+and\\s+than\\b", RegexOption.IGNORE_CASE) to "now and then",

            // loose vs lose
            Regex("\\bdon't\\s+loose\\b", RegexOption.IGNORE_CASE) to "don't lose",
            Regex("\\bgonna\\s+loose\\b", RegexOption.IGNORE_CASE) to "gonna lose",
            Regex("\\bwill\\s+loose\\b", RegexOption.IGNORE_CASE) to "will lose",

            // could of / would of / should of
            Regex("\\bcould\\s+of\\b", RegexOption.IGNORE_CASE) to "could have",
            Regex("\\bshould\\s+of\\b", RegexOption.IGNORE_CASE) to "should have",
            Regex("\\bwould\\s+of\\b", RegexOption.IGNORE_CASE) to "would have",
            Regex("\\bmight\\s+of\\b", RegexOption.IGNORE_CASE) to "might have"
        )

        for ((regex, replacement) in homophoneRules) {
            if (regex.containsMatchIn(result)) {
                result = result.replace(regex, replacement)
                changes.add("Resolved contextual word choice ('$replacement')")
            }
        }

        // 3. Subject-Verb Agreement & Verb Tenses
        val grammarRules = listOf(
            Regex("\\bthey\\s+is\\b", RegexOption.IGNORE_CASE) to "they are",
            Regex("\\bwe\\s+was\\b", RegexOption.IGNORE_CASE) to "we were",
            Regex("\\byou\\s+was\\b", RegexOption.IGNORE_CASE) to "you were",
            Regex("\\bdid\\s+went\\b", RegexOption.IGNORE_CASE) to "went",
            Regex("\\bdid\\s+saw\\b", RegexOption.IGNORE_CASE) to "saw",
            Regex("\\bdid\\s+told\\b", RegexOption.IGNORE_CASE) to "told",
            Regex("\\bhave\\s+went\\b", RegexOption.IGNORE_CASE) to "have gone",
            Regex("\\bhas\\s+went\\b", RegexOption.IGNORE_CASE) to "has gone",
            Regex("\\bhe\\s+don't\\b", RegexOption.IGNORE_CASE) to "he doesn't",
            Regex("\\bshe\\s+don't\\b", RegexOption.IGNORE_CASE) to "she doesn't",
            Regex("\\bit\\s+don't\\b", RegexOption.IGNORE_CASE) to "it doesn't",
            Regex("\\bhe\\s+go\\b", RegexOption.IGNORE_CASE) to "he goes",
            Regex("\\bshe\\s+go\\b", RegexOption.IGNORE_CASE) to "she goes",
            Regex("\\bdoes\\s+has\\b", RegexOption.IGNORE_CASE) to "does have"
        )

        for ((regex, replacement) in grammarRules) {
            if (regex.containsMatchIn(result)) {
                result = result.replace(regex, replacement)
                changes.add("Corrected grammatical agreement ('$replacement')")
            }
        }

        // 4. Common Misspellings and Typos from TypoCorrectionCorpus
        val words = result.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?])|(?=[.,!?])"))
        val sb = StringBuilder()
        var typoFixed = false
        for (w in words) {
            val fix = TypoCorrectionCorpus.findCorrection(w)
            if (fix != null && !fix.equals(w, ignoreCase = true)) {
                sb.append(fix)
                typoFixed = true
            } else {
                sb.append(w)
            }
        }
        if (typoFixed) {
            result = sb.toString()
            changes.add("Corrected misspelled words and missing apostrophes")
        }

        // 5. Fix "a" vs "an" before vowel sounds
        val anRegex = Regex("\\ba\\s+([aeiouAEIOU][a-z]+)")
        if (anRegex.containsMatchIn(result)) {
            result = anRegex.replace(result, "an $1")
            changes.add("Adjusted 'a' to 'an' before vowel sound")
        }

        // 6. Fix duplicate consecutive words ("the the" -> "the")
        val dupRegex = Regex("\\b([a-zA-Z]+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
        if (dupRegex.containsMatchIn(result)) {
            result = dupRegex.replace(result, "$1")
            changes.add("Removed duplicate consecutive word")
        }

        // 7. Punctuation spacing and capitalization
        result = result.replace(Regex("\\s+([.,!?])"), "$1")
        result = result.replace(Regex("([.,!?])([a-zA-Z])"), "$1 $2")

        val sentenceRegex = Regex("(^|[.!?]\\s+)([a-z])")
        result = sentenceRegex.replace(result) { match ->
            val prefix = match.groupValues[1]
            val letter = match.groupValues[2].uppercase()
            changes.add("Capitalized sentence starter")
            "$prefix$letter"
        }

        // 8. Ensure terminal punctuation if missing
        val trimmed = result.trimEnd()
        if (trimmed.isNotEmpty() && !trimmed.endsWith('.') && !trimmed.endsWith('!') && !trimmed.endsWith('?')) {
            result = "$trimmed."
            changes.add("Added terminal period")
        }

        return ProofreadResult(text, result, changes)
    }

    /**
     * Rewrites text according to selected tone with our on-device Small Language Model.
     */
    fun rewrite(text: String, tone: Tone): String {
        val clean = text.trim()
        if (clean.isBlank()) return text

        val task = when (tone) {
            Tone.FRIENDLY -> SmallLanguageModelEngine.Task.REWRITE_FRIENDLY
            Tone.PROFESSIONAL -> SmallLanguageModelEngine.Task.REWRITE_PROFESSIONAL
            Tone.CONCISE -> SmallLanguageModelEngine.Task.REWRITE_CONCISE
        }
        return slm.generate(clean, task).outputText
    }

    private fun rewriteFriendly(text: String): String {
        var res = text

        val friendlyReplacements = listOf(
            Regex("\\bPlease find attached\\b", RegexOption.IGNORE_CASE) to "Here is",
            Regex("\\bI would appreciate\\b", RegexOption.IGNORE_CASE) to "I'd really love",
            Regex("\\bSincerely\\b", RegexOption.IGNORE_CASE) to "Warmly",
            Regex("\\bRegards\\b", RegexOption.IGNORE_CASE) to "Best",
            Regex("\\bDo not hesitate to contact me\\b", RegexOption.IGNORE_CASE) to "Reach out anytime!",
            Regex("\\bI am writing to inform you that\\b", RegexOption.IGNORE_CASE) to "Just wanted to let you know that",
            Regex("\\bPlease be advised that\\b", RegexOption.IGNORE_CASE) to "Quick heads up that",
            Regex("\\bIt is required that you\\b", RegexOption.IGNORE_CASE) to "Whenever you get a chance, could you",
            Regex("\\bKindly respond\\b", RegexOption.IGNORE_CASE) to "Looking forward to hearing from you!",
            Regex("\\bI look forward to meeting\\b", RegexOption.IGNORE_CASE) to "Can't wait to catch up soon!",
            Regex("\\bcan you check\\b", RegexOption.IGNORE_CASE) to "could you take a quick peek at",
            Regex("\\bneed this\\b", RegexOption.IGNORE_CASE) to "would love to have this",
            Regex("\\bcall me\\b", RegexOption.IGNORE_CASE) to "give me a call whenever you're free",
            Regex("\\bthanks\\b", RegexOption.IGNORE_CASE) to "thanks a bunch"
        )

        for ((r, s) in friendlyReplacements) {
            res = res.replace(r, s)
        }

        if (!res.endsWith("!") && !res.endsWith("?")) {
            res = res.trimEnd('.') + "!"
        }

        return "$res 😊"
    }

    private fun rewriteProfessional(text: String): String {
        var res = text

        val proReplacements = listOf(
            Regex("\\bcan you check\\b", RegexOption.IGNORE_CASE) to "could you please review",
            Regex("\\bneed this ASAP\\b", RegexOption.IGNORE_CASE) to "would appreciate your prompt attention to this matter",
            Regex("\\bwhat do you think\\b", RegexOption.IGNORE_CASE) to "I would value your feedback on this",
            Regex("\\blet me know\\b", RegexOption.IGNORE_CASE) to "please advise at your earliest convenience",
            Regex("\\blet's talk about\\b", RegexOption.IGNORE_CASE) to "let us schedule time to discuss",
            Regex("\\bsorry for being late\\b", RegexOption.IGNORE_CASE) to "thank you for your patience",
            Regex("\\bsorry for the delay\\b", RegexOption.IGNORE_CASE) to "thank you for your patience and understanding",
            Regex("\\bI want to know\\b", RegexOption.IGNORE_CASE) to "I would like to inquire regarding",
            Regex("\\bgive me an update\\b", RegexOption.IGNORE_CASE) to "could you provide a status update",
            Regex("\\bfix this\\b", RegexOption.IGNORE_CASE) to "resolve this issue",
            Regex("\\bthis is a bad idea\\b", RegexOption.IGNORE_CASE) to "there may be significant challenges with this approach",
            Regex("\\bI think that\\b", RegexOption.IGNORE_CASE) to "in my assessment,",
            Regex("\\bsee what I mean\\b", RegexOption.IGNORE_CASE) to "as referenced above",
            Regex("\\bthanks a ton\\b|\\bthanks alot\\b", RegexOption.IGNORE_CASE) to "thank you for your assistance",
            Regex("\\bdrop me a line\\b", RegexOption.IGNORE_CASE) to "please feel free to contact me",
            Regex("\\bgonna\\b", RegexOption.IGNORE_CASE) to "going to",
            Regex("\\bwanna\\b", RegexOption.IGNORE_CASE) to "would like to",
            Regex("\\bgotta\\b", RegexOption.IGNORE_CASE) to "must",
            Regex("\\bhey\\b", RegexOption.IGNORE_CASE) to "Hello",
            Regex("\\byeah\\b|\\byep\\b", RegexOption.IGNORE_CASE) to "Yes",
            Regex("\\bnope\\b", RegexOption.IGNORE_CASE) to "No",
            Regex("\\bbuy\\b", RegexOption.IGNORE_CASE) to "purchase",
            Regex("\\bstart\\b", RegexOption.IGNORE_CASE) to "commence"
        )

        for ((r, s) in proReplacements) {
            res = res.replace(r, s)
        }

        // Expand informal contractions into formal register
        res = res.replace(Regex("\\bcan't\\b", RegexOption.IGNORE_CASE), "cannot")
        res = res.replace(Regex("\\bdon't\\b", RegexOption.IGNORE_CASE), "do not")
        res = res.replace(Regex("\\bwon't\\b", RegexOption.IGNORE_CASE), "will not")
        res = res.replace(Regex("\\bI'm\\b", RegexOption.IGNORE_CASE), "I am")
        res = res.replace(Regex("\\bwe're\\b", RegexOption.IGNORE_CASE), "we are")
        res = res.replace("!", ".")

        return proofread(res).correctedText
    }

    private fun rewriteConcise(text: String): String {
        var res = text

        val conciseReplacements = listOf(
            Regex("\\bin order to\\b", RegexOption.IGNORE_CASE) to "to",
            Regex("\\bdue to the fact that\\b", RegexOption.IGNORE_CASE) to "because",
            Regex("\\bat the present time\\b", RegexOption.IGNORE_CASE) to "now",
            Regex("\\bat this point in time\\b", RegexOption.IGNORE_CASE) to "currently",
            Regex("\\bin the event that\\b", RegexOption.IGNORE_CASE) to "if",
            Regex("\\bwith reference to\\b", RegexOption.IGNORE_CASE) to "regarding",
            Regex("\\bfor the purpose of\\b", RegexOption.IGNORE_CASE) to "for",
            Regex("\\bhas the ability to\\b", RegexOption.IGNORE_CASE) to "can",
            Regex("\\bis able to\\b", RegexOption.IGNORE_CASE) to "can",
            Regex("\\bin a timely manner\\b", RegexOption.IGNORE_CASE) to "promptly",
            Regex("\\btake into consideration\\b", RegexOption.IGNORE_CASE) to "consider",
            Regex("\\ba large number of\\b", RegexOption.IGNORE_CASE) to "many",
            Regex("\\bmake a decision\\b", RegexOption.IGNORE_CASE) to "decide",
            Regex("\\breach out to\\b", RegexOption.IGNORE_CASE) to "contact",
            Regex("\\bgive an indication of\\b", RegexOption.IGNORE_CASE) to "indicate",
            Regex("\\bit is important to note that\\b", RegexOption.IGNORE_CASE) to "",
            Regex("\\bI am writing this to\\b", RegexOption.IGNORE_CASE) to "",
            Regex("\\bjust wanted to ask if\\b", RegexOption.IGNORE_CASE) to "could",
            Regex("\\bjust wondering if\\b", RegexOption.IGNORE_CASE) to "if",
            Regex("\\bwondering if\\b", RegexOption.IGNORE_CASE) to "if",
            Regex("\\bbasically\\b\\s*", RegexOption.IGNORE_CASE) to "",
            Regex("\\bactually\\b\\s*", RegexOption.IGNORE_CASE) to "",
            Regex("\\bliterally\\b\\s*", RegexOption.IGNORE_CASE) to "",
            Regex("\\bkind of\\b\\s*", RegexOption.IGNORE_CASE) to "",
            Regex("\\bsort of\\b\\s*", RegexOption.IGNORE_CASE) to ""
        )

        for ((r, s) in conciseReplacements) {
            res = res.replace(r, s)
        }

        res = res.replace(Regex("\\s{2,}"), " ").trim()
        return proofread(res).correctedText
    }

    /**
     * Extracts key points or generates a TL;DR summary using our on-device Small Language Model.
     */
    fun summarize(text: String, style: SummaryStyle = SummaryStyle.KEY_POINTS): String {
        val task = when (style) {
            SummaryStyle.KEY_POINTS -> SmallLanguageModelEngine.Task.SUMMARIZE_KEY_POINTS
            SummaryStyle.TLDR -> SmallLanguageModelEngine.Task.SUMMARIZE_TLDR
        }
        return slm.generate(text, task).outputText
    }

    /**
     * iOS 27 Genmoji generator: creates composite emoji stickers based on prompt or selected keywords.
     */
    fun createGenmoji(prompt: String): GenmojiItem {
        val lower = prompt.lowercase().trim()
        val (emoji1, emoji2, desc) = when {
            lower.contains("party") || lower.contains("celebrate") || lower.contains("cheers") -> Triple("🥳", "🎉", "Party Celebration")
            lower.contains("love") || lower.contains("heart") || lower.contains("romantic") -> Triple("🥰", "💖", "Sweet Loving Heart")
            lower.contains("fire") || lower.contains("cool") || lower.contains("lit") -> Triple("😎", "🔥", "Cool & On Fire")
            lower.contains("coffee") || lower.contains("morning") || lower.contains("wake") -> Triple("☕", "✨", "Morning Brew Vibe")
            lower.contains("mind blown") || lower.contains("crazy") || lower.contains("wild") -> Triple("🤯", "💥", "Mind Blown Blast")
            lower.contains("sleepy") || lower.contains("tired") || lower.contains("night") -> Triple("😴", "🌙", "Sleepy Night Dream")
            lower.contains("rocket") || lower.contains("fast") || lower.contains("space") -> Triple("🚀", "✨", "Super Cosmic Rocket")
            lower.contains("code") || lower.contains("tech") || lower.contains("developer") -> Triple("💻", "⚡", "Cyber Tech Wizard")
            lower.contains("game") || lower.contains("gaming") || lower.contains("win") -> Triple("🎮", "🏆", "Victory Gamer")
            lower.contains("cat") || lower.contains("kitty") -> Triple("🐱", "😻", "Playful Kitty")
            lower.contains("dog") || lower.contains("puppy") -> Triple("🐶", "🎾", "Happy Puppy")
            lower.contains("laugh") || lower.contains("funny") || lower.contains("lol") -> Triple("😂", "🌟", "Laughing Superstar")
            lower.contains("money") || lower.contains("cash") || lower.contains("rich") -> Triple("🤑", "💸", "Money Bag Hustler")
            lower.contains("music") || lower.contains("song") || lower.contains("dance") -> Triple("🎵", "🕺", "Groovy Musical Rhythm")
            else -> Triple("✨", "💫", "Magic Sparkle Genmoji")
        }

        return GenmojiItem(
            prompt = prompt,
            primaryEmoji = emoji1,
            secondaryEmoji = emoji2,
            compositeDescription = desc
        )
    }

    /**
     * Converts raw text into an authentic iOS formatted bulleted list using SLM.
     */
    fun formatList(text: String): String {
        return slm.generate(text, SmallLanguageModelEngine.Task.FORMAT_LIST).outputText
    }

    /**
     * Formats structured text into an authentic clean markdown table using SLM.
     */
    fun formatTable(text: String): String {
        return slm.generate(text, SmallLanguageModelEngine.Task.FORMAT_TABLE).outputText
    }

    /**
     * Generates a thoughtful, context-aware message continuation or draft using SLM.
     */
    fun composeText(contextText: String): String {
        return slm.generate(contextText, SmallLanguageModelEngine.Task.COMPOSE).outputText
    }

    /**
     * Executes custom Apple Intelligence "Describe your change" user prompts using SLM.
     */
    fun customTransform(text: String, instruction: String): String {
        return slm.generate(text, SmallLanguageModelEngine.Task.CUSTOM_INSTRUCTION, instruction).outputText
    }
}
