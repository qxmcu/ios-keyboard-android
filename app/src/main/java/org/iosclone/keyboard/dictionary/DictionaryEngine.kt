package org.iosclone.keyboard.dictionary

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.iosclone.keyboard.layout.LanguageLayout
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Intelligent, low-latency Autocorrect & QuickType Engine for iOS 27 Keyboard.
 * Combines quantized n-gram language model, spatial key distance matrix,
 * contraction expansion, predictive emojis, and dynamic user learning.
 */
class DictionaryEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tries = mutableMapOf<LanguageLayout, Trie>()
    private val symSpells = mutableMapOf<LanguageLayout, SymSpell>()
    private val userDb = UserDictionaryDb(context)

    val languageModel = QuantizedLanguageModel(context)
    val textReplacementManager = TextReplacementManager(context)
    val undoManager = AutocorrectUndoManager(userDb)

    private var currentLanguage: LanguageLayout = LanguageLayout.QWERTY

    init {
        loadLanguage(LanguageLayout.QWERTY)
    }

    fun setLanguage(language: LanguageLayout) {
        currentLanguage = language
        if (!tries.containsKey(language)) {
            loadLanguage(language)
        }
    }

    private fun loadLanguage(language: LanguageLayout) {
        scope.launch {
            val trie = Trie()
            val symSpell = SymSpell()
            val fileName = when (language) {
                LanguageLayout.QWERTY -> "en_words.txt"
                LanguageLayout.SPANISH -> "es_words.txt"
                LanguageLayout.AZERTY -> "fr_words.txt"
                LanguageLayout.QWERTZ -> "de_words.txt"
                else -> "en_words.txt"
            }

            try {
                withContext(Dispatchers.IO) {
                    context.assets.open("dictionaries/$fileName").use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                            var symSpellCount = 0
                            for (line in lines) {
                                val parts = line.split("\t")
                                if (parts.isNotEmpty()) {
                                    val word = parts[0].trim()
                                    val freq = if (parts.size > 1) parts[1].toIntOrNull() ?: 10 else 10
                                    trie.insert(word, freq)
                                    if (symSpellCount < 50000) {
                                        symSpell.createDictionaryEntry(word, freq.toLong())
                                        symSpellCount++
                                    }
                                }
                            }
                        }
                    }

                    // Load user-learned words
                    val userWords = userDb.getAllWordsForLanguage(language.name)
                    for ((word, freq) in userWords) {
                        trie.insert(word, freq)
                        symSpell.createDictionaryEntry(word, freq.toLong())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            synchronized(tries) {
                tries[language] = trie
                symSpells[language] = symSpell
            }
        }
    }

    /**
     * Resolves predictions and autocorrection for the current typed word and context.
     * @param rawInput The word currently being typed (can be empty after space).
     * @param previousWords Recent words in the sentence for n-gram context.
     * @param autocorrectEnabled Preference flag.
     */
    fun getSuggestions(
        rawInput: String,
        previousWords: List<String> = emptyList(),
        autocorrectEnabled: Boolean = true,
        touchPoints: List<TouchPoint>? = null,
        keyMatrix: Map<Char, KeyCenter>? = null
    ): AutocorrectResult {
        val prevWord = previousWords.lastOrNull()?.trim()

        // 1. Next-word prediction when rawInput is empty (e.g. right after a space)
        if (rawInput.isBlank()) {
            if (previousWords.isEmpty()) return AutocorrectResult.Empty

            val nextPredictions = languageModel.predictNextWords(previousWords, limit = 3)
            val suggestedEmoji = if (prevWord != null) PredictiveEmojiEngine.getSuggestedEmojis(prevWord)?.firstOrNull() else null

            val left = nextPredictions.getOrNull(0) ?: ""
            val center = nextPredictions.getOrNull(1) ?: ""
            val right = suggestedEmoji ?: nextPredictions.getOrNull(2) ?: ""

            return AutocorrectResult(
                centerCandidate = center,
                leftCandidate = left,
                rightCandidate = right,
                isExactMatch = true,
                rawTypedWord = "",
                isAutocorrectCandidate = false,
                suggestedEmoji = suggestedEmoji
            )
        }

        // 2. Check Text Replacement Shortcuts (e.g. "omw" -> "On my way!")
        val shortcutReplacement = textReplacementManager.getReplacement(rawInput)
        if (shortcutReplacement != null) {
            return AutocorrectResult(
                centerCandidate = shortcutReplacement,
                leftCandidate = rawInput,
                rightCandidate = "",
                isExactMatch = false,
                rawTypedWord = rawInput,
                isAutocorrectCandidate = true,
                shortcutReplacement = shortcutReplacement
            )
        }

        // 3. Check TypoCorrectionCorpus & Contractions ("dont" -> "don't", "teh" -> "the", "recieve" -> "receive")
        val typoFix = TypoCorrectionCorpus.findCorrection(rawInput) ?: ContractionFixer.getFix(rawInput)
        if (typoFix != null && autocorrectEnabled && !undoManager.isIgnored(rawInput)) {
            val emoji = PredictiveEmojiEngine.getSuggestedEmojis(typoFix)?.firstOrNull()
            return AutocorrectResult(
                centerCandidate = typoFix,
                leftCandidate = rawInput,
                rightCandidate = emoji ?: "",
                isExactMatch = false,
                rawTypedWord = rawInput,
                isAutocorrectCandidate = true,
                suggestedEmoji = emoji
            )
        }

        val trie = synchronized(tries) { tries[currentLanguage] } ?: return AutocorrectResult(
            centerCandidate = rawInput,
            leftCandidate = "",
            rightCandidate = "",
            isExactMatch = true,
            rawTypedWord = rawInput
        )

        val lowerInput = rawInput.lowercase()
        val isAllUpper = rawInput.length > 1 && rawInput.all { it.isUpperCase() }
        val isCapitalized = rawInput[0].isUpperCase()

        val isExact = trie.contains(lowerInput)

        // 4. Prefix matches from Trie
        val prefixMatches = trie.findPrefixSuggestions(lowerInput, limit = 8)

        // 5. Symmetric Delete (SymSpell) + Trie Fuzzy matches
        val symSpell = synchronized(tries) { symSpells[currentLanguage] }
        val fuzzyCandidates = mutableListOf<Pair<String, Int>>()
        if (symSpell != null && !isExact && autocorrectEnabled && rawInput.length >= 2 && !undoManager.isIgnored(rawInput)) {
            val symResults = symSpell.lookup(lowerInput, maxEditDistance = 2, limit = 12)
            for (item in symResults) {
                fuzzyCandidates.add(Pair(item.term, item.count.toInt().coerceAtMost(255)))
            }
        }
        if (fuzzyCandidates.isEmpty() && !isExact && autocorrectEnabled && rawInput.length >= 2 && !undoManager.isIgnored(rawInput)) {
            fuzzyCandidates.addAll(trie.searchFuzzy(lowerInput, maxCost = 2, limit = 8))
        }

        // Combine and score candidates with Language Model + Spatial Key Proximity
        data class ScoredCandidate(val word: String, val score: Float)

        val candidatePool = (prefixMatches + fuzzyCandidates).distinctBy { it.first }
        val scoredList = candidatePool.map { (word, freq) ->
            val spatialDist = SpatialKeyDistance.spatialDistance(lowerInput, word, touchPoints, keyMatrix)
            val contextScore = languageModel.getContextScore(word, prevWord)
            val isPrefix = word.startsWith(lowerInput)

            // Weight calculation:
            // High frequency + high context boost - spatial Euclidean penalty
            var score = (freq * 0.35f) + (contextScore * 0.5f) - (spatialDist * 85f)
            if (isPrefix) score += 60f
            if (word.equals(lowerInput, ignoreCase = true)) score += 500f

            ScoredCandidate(word, score)
        }.sortedByDescending { it.score }

        // Format casing
        fun applyCasing(word: String): String {
            return when {
                isAllUpper -> word.uppercase()
                isCapitalized && word.isNotEmpty() -> word.replaceFirstChar { it.uppercase() }
                else -> word
            }
        }

        val topWord = scoredList.firstOrNull()?.word?.let { applyCasing(it) }
        val secondWord = scoredList.getOrNull(1)?.word?.let { applyCasing(it) }
        val thirdWord = scoredList.getOrNull(2)?.word?.let { applyCasing(it) }

        // Predictive Emoji match
        val emojiMatch = PredictiveEmojiEngine.getSuggestedEmojis(topWord ?: rawInput)?.firstOrNull()

        val centerCandidate: String
        val leftCandidate: String
        val rightCandidate: String
        val willAutocorrect: Boolean

        if (isExact) {
            centerCandidate = rawInput
            leftCandidate = secondWord?.takeIf { !it.equals(rawInput, ignoreCase = true) } ?: ""
            rightCandidate = emojiMatch ?: thirdWord?.takeIf { !it.equals(rawInput, ignoreCase = true) && !it.equals(leftCandidate, ignoreCase = true) } ?: ""
            willAutocorrect = false
        } else {
            if (autocorrectEnabled && topWord != null && !topWord.equals(rawInput, ignoreCase = true) && !undoManager.isIgnored(rawInput)) {
                centerCandidate = topWord
                leftCandidate = rawInput // In iOS, verbatim typed word is on the left
                rightCandidate = emojiMatch ?: secondWord ?: ""
                willAutocorrect = true
            } else {
                centerCandidate = rawInput
                leftCandidate = topWord ?: ""
                rightCandidate = emojiMatch ?: secondWord ?: ""
                willAutocorrect = false
            }
        }

        // Inline ghost text prediction
        val inlinePrediction = if (topWord != null && topWord.lowercase().startsWith(lowerInput) && topWord.length > rawInput.length) {
            topWord.substring(rawInput.length)
        } else {
            null
        }

        return AutocorrectResult(
            centerCandidate = centerCandidate,
            leftCandidate = leftCandidate,
            rightCandidate = rightCandidate,
            isExactMatch = isExact,
            rawTypedWord = rawInput,
            isAutocorrectCandidate = willAutocorrect,
            suggestedEmoji = emojiMatch,
            inlinePrediction = inlinePrediction
        )
    }

    /**
     * Learns words and bigram associations from committed text.
     */
    fun onWordCommitted(word: String, previousWord: String? = null) {
        val trimmed = word.trim()
        if (trimmed.length in 2..32 && trimmed.all { it.isLetter() || it == '\'' }) {
            scope.launch(Dispatchers.IO) {
                userDb.insertOrIncrementWord(trimmed, currentLanguage.name)
                synchronized(tries) {
                    tries[currentLanguage]?.insert(trimmed.lowercase(), 120)
                }
                if (previousWord != null && previousWord.isNotBlank()) {
                    languageModel.learnBigram(previousWord, trimmed)
                }
            }
        }
    }

    fun getTrie(language: LanguageLayout): Trie? {
        return synchronized(tries) { tries[language] }
    }
}
