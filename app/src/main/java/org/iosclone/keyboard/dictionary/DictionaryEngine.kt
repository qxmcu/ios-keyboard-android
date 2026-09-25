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

class DictionaryEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tries = mutableMapOf<LanguageLayout, Trie>()
    private val userDb = UserDictionaryDb(context)

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
                            for (line in lines) {
                                val parts = line.split("\t")
                                if (parts.isNotEmpty()) {
                                    val word = parts[0].trim()
                                    val freq = if (parts.size > 1) parts[1].toIntOrNull() ?: 10 else 10
                                    trie.insert(word, freq)
                                }
                            }
                        }
                    }

                    // Load user-learned words
                    val userWords = userDb.getAllWordsForLanguage(language.name)
                    for ((word, freq) in userWords) {
                        trie.insert(word, freq)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            synchronized(tries) {
                tries[language] = trie
            }
        }
    }

    /**
     * Resolves predictions and autocorrection for the current typed word.
     */
    fun getSuggestions(rawInput: String, autocorrectEnabled: Boolean): AutocorrectResult {
        if (rawInput.isBlank()) {
            return AutocorrectResult.Empty
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

        // 1. Prefix matches
        val prefixMatches = trie.findPrefixSuggestions(lowerInput, limit = 5)

        // 2. Fuzzy matches if no prefix matches or not exact and autocorrect enabled
        val candidates = if (prefixMatches.isNotEmpty()) {
            prefixMatches.map { it.first }
        } else if (autocorrectEnabled) {
            trie.searchFuzzy(lowerInput, maxCost = 2, limit = 5).map { it.first }
        } else {
            emptyList()
        }

        // Format casing to match user input
        fun applyCasing(word: String): String {
            return when {
                isAllUpper -> word.uppercase()
                isCapitalized && word.isNotEmpty() -> word.replaceFirstChar { it.uppercase() }
                else -> word
            }
        }

        val formattedCandidates = candidates.map { applyCasing(it) }

        val centerCandidate: String
        val leftCandidate: String
        val rightCandidate: String

        if (isExact) {
            centerCandidate = rawInput
            leftCandidate = formattedCandidates.getOrNull(0)?.takeIf { !it.equals(rawInput, ignoreCase = true) }
                ?: formattedCandidates.getOrNull(1) ?: ""
            rightCandidate = formattedCandidates.getOrNull(1)?.takeIf { !it.equals(rawInput, ignoreCase = true) && !it.equals(leftCandidate, ignoreCase = true) }
                ?: formattedCandidates.getOrNull(2) ?: ""
        } else {
            // Autocorrect kicks in for center candidate if autocorrect is enabled
            val topSuggested = formattedCandidates.firstOrNull()
            if (autocorrectEnabled && topSuggested != null) {
                centerCandidate = topSuggested
                leftCandidate = rawInput // Left column shows verbatim what user typed in iOS
                rightCandidate = formattedCandidates.getOrNull(1) ?: ""
            } else {
                centerCandidate = rawInput
                leftCandidate = formattedCandidates.getOrNull(0) ?: ""
                rightCandidate = formattedCandidates.getOrNull(1) ?: ""
            }
        }

        return AutocorrectResult(
            centerCandidate = centerCandidate,
            leftCandidate = leftCandidate,
            rightCandidate = rightCandidate,
            isExactMatch = isExact,
            rawTypedWord = rawInput
        )
    }

    /**
     * Learns a newly committed word in user dictionary
     */
    fun learnWord(word: String) {
        val trimmed = word.trim()
        if (trimmed.length in 2..32 && trimmed.all { it.isLetter() }) {
            scope.launch(Dispatchers.IO) {
                userDb.insertOrIncrementWord(trimmed, currentLanguage.name)
                synchronized(tries) {
                    tries[currentLanguage]?.insert(trimmed.lowercase(), 100)
                }
            }
        }
    }

    fun getTrie(language: LanguageLayout): Trie? {
        return synchronized(tries) { tries[language] }
    }
}
