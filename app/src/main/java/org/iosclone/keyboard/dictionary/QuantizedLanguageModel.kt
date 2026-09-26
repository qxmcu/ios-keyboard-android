package org.iosclone.keyboard.dictionary

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * High-performance, quantized n-gram language model for low-latency next-word and phrase prediction.
 * Uses 8-bit quantized log probabilities (0..255) for minimal memory footprint (<1MB)
 * and sub-millisecond evaluation with zero battery impact.
 */
class QuantizedLanguageModel(private val context: Context? = null) {

    // Bigram map: prevWord -> list of (nextWord, quantizedScore: Byte)
    private val staticBigrams = mutableMapOf<String, MutableList<Pair<String, Byte>>>()

    // Trigram map: (w1 + " " + w2) -> list of (w3, quantizedScore: Byte)
    private val staticTrigrams = mutableMapOf<String, MutableList<Pair<String, Byte>>>()

    // Dynamic user-learned bigrams: prevWord -> (nextWord -> frequency)
    private val dynamicUserBigrams = mutableMapOf<String, MutableMap<String, Int>>()

    init {
        loadConversationalCorpus()
        loadAssetTransitions()
    }

    private fun loadAssetTransitions() {
        val ctx = context ?: return
        try {
            ctx.assets.open("models/lm_transitions.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    for (line in lines) {
                        val parts = line.split("\t")
                        if (parts.isNotEmpty()) {
                            val phrase = parts[0].trim().lowercase()
                            val score = if (parts.size > 1) parts[1].toIntOrNull() ?: 230 else 230
                            val words = phrase.split("\\s+".toRegex()).filter { it.isNotBlank() }
                            when (words.size) {
                                2 -> addBigram(words[0], words[1], score)
                                3 -> addTrigram(words[0], words[1], words[2], score)
                                4 -> {
                                    addTrigram("${words[0]} ${words[1]}", words[2], words[3], score)
                                    addTrigram(words[1], words[2], words[3], score)
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Predicts the most likely next words given the 1 or 2 preceding words.
     * Combines trigram context, bigram context, and user-learned patterns.
     */
    fun predictNextWords(previousWords: List<String>, limit: Int = 3): List<String> {
        if (previousWords.isEmpty()) return listOf("I", "The", "How")

        val results = mutableMapOf<String, Int>()

        val lastWord = previousWords.last().lowercase().trim()
        val secondLastWord = if (previousWords.size >= 2) previousWords[previousWords.size - 2].lowercase().trim() else null

        // 1. Trigram evaluation if 2 previous words exist
        if (secondLastWord != null) {
            val trigramKey = "$secondLastWord $lastWord"
            staticTrigrams[trigramKey]?.forEach { (w3, score) ->
                results[w3] = (score.toInt() and 0xFF) + 120 // heavy weight for trigram match
            }
        }

        // 2. User dynamic bigrams (personalized learning takes high priority)
        synchronized(dynamicUserBigrams) {
            dynamicUserBigrams[lastWord]?.forEach { (nextWord, freq) ->
                val existing = results[nextWord] ?: 0
                results[nextWord] = existing + (freq * 15).coerceAtMost(180)
            }
        }

        // 3. Static Quantized Bigrams
        staticBigrams[lastWord]?.forEach { (nextWord, score) ->
            val existing = results[nextWord] ?: 0
            results[nextWord] = maxOf(existing, score.toInt() and 0xFF)
        }

        return results.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
    }

    /**
     * Returns a context score (0..255) for candidate word given previous word context.
     * Used by autocorrect engine to rank candidate words realistically in sentence context.
     */
    fun getContextScore(candidate: String, prevWord: String?): Int {
        if (prevWord == null) return 0
        val p = prevWord.lowercase().trim()
        val c = candidate.lowercase().trim()

        // Check user learned bigrams first
        synchronized(dynamicUserBigrams) {
            val userFreq = dynamicUserBigrams[p]?.get(c)
            if (userFreq != null) {
                return (userFreq * 20).coerceAtMost(250)
            }
        }

        // Check static bigrams
        val list = staticBigrams[p] ?: return 0
        val match = list.firstOrNull { it.first.equals(c, ignoreCase = true) }
        return if (match != null) (match.second.toInt() and 0xFF) else 0
    }

    /**
     * Learns a new bigram transition from user typing.
     */
    fun learnBigram(w1: String, w2: String) {
        val p1 = w1.lowercase().trim()
        val p2 = w2.lowercase().trim()
        if (p1.isEmpty() || p2.isEmpty() || !p1.all { it.isLetter() } || !p2.all { it.isLetter() }) return

        synchronized(dynamicUserBigrams) {
            val map = dynamicUserBigrams.getOrPut(p1) { mutableMapOf() }
            map[p2] = (map[p2] ?: 0) + 1
        }
    }

    private fun addBigram(w1: String, w2: String, quantizedScore: Int) {
        val byteScore = quantizedScore.coerceIn(1, 255).toByte()
        val list = staticBigrams.getOrPut(w1) { mutableListOf() }
        if (list.none { it.first == w2 }) {
            list.add(Pair(w2, byteScore))
        }
    }

    private fun addTrigram(w1: String, w2: String, w3: String, quantizedScore: Int) {
        val byteScore = quantizedScore.coerceIn(1, 255).toByte()
        val key = "$w1 $w2"
        val list = staticTrigrams.getOrPut(key) { mutableListOf() }
        if (list.none { it.first == w3 }) {
            list.add(Pair(w3, byteScore))
        }
    }

    private fun loadConversationalCorpus() {
        // High-frequency English conversational bigrams and trigrams
        addBigram("how", "are", 240)
        addBigram("how", "is", 220)
        addBigram("how", "was", 200)
        addTrigram("how", "are", "you", 255)
        addTrigram("how", "are", "things", 210)
        addTrigram("how", "is", "it", 240)
        addTrigram("how", "is", "your", 220)

        // "thank you"
        addBigram("thank", "you", 255)
        addTrigram("thank", "you", "so", 240)
        addTrigram("thank", "you", "very", 220)
        addTrigram("thank", "you", "for", 230)
        addTrigram("thank", "you", "much", 210)

        // "i am", "i have", "i will", "i want"
        addBigram("i", "am", 245)
        addBigram("i", "have", 240)
        addBigram("i", "will", 235)
        addBigram("i", "want", 220)
        addBigram("i", "can", 230)
        addBigram("i", "think", 215)
        addBigram("i", "love", 210)
        addBigram("i", "know", 205)
        addBigram("i", "need", 200)
        addTrigram("i", "am", "going", 250)
        addTrigram("i", "am", "so", 230)
        addTrigram("i", "am", "at", 220)
        addTrigram("i", "am", "ready", 215)
        addTrigram("i", "have", "to", 250)
        addTrigram("i", "have", "a", 240)
        addTrigram("i", "have", "been", 235)
        addTrigram("i", "want", "to", 255)
        addTrigram("i", "will", "be", 250)
        addTrigram("i", "will", "call", 230)
        addTrigram("i", "need", "to", 250)
        addTrigram("i", "love", "you", 255)
        addTrigram("i", "know", "right", 240)
        addBigram("on", "my", 250)
        addTrigram("on", "my", "way", 255)
    }
}
