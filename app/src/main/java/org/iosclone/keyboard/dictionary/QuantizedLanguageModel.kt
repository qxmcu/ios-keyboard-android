package org.iosclone.keyboard.dictionary

/**
 * High-performance, quantized n-gram language model for low-latency next-word and phrase prediction.
 * Uses 8-bit quantized log probabilities (0..255) for minimal memory footprint (<1MB)
 * and sub-millisecond evaluation with zero battery impact.
 */
class QuantizedLanguageModel {

    // Bigram map: prevWord -> list of (nextWord, quantizedScore: Byte)
    private val staticBigrams = mutableMapOf<String, MutableList<Pair<String, Byte>>>()

    // Trigram map: (w1 + " " + w2) -> list of (w3, quantizedScore: Byte)
    private val staticTrigrams = mutableMapOf<String, MutableList<Pair<String, Byte>>>()

    // Dynamic user-learned bigrams: prevWord -> (nextWord -> frequency)
    private val dynamicUserBigrams = mutableMapOf<String, MutableMap<String, Int>>()

    init {
        loadConversationalCorpus()
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
        list.add(Pair(w2, byteScore))
    }

    private fun addTrigram(w1: String, w2: String, w3: String, quantizedScore: Int) {
        val byteScore = quantizedScore.coerceIn(1, 255).toByte()
        val key = "$w1 $w2"
        val list = staticTrigrams.getOrPut(key) { mutableListOf() }
        list.add(Pair(w3, byteScore))
    }

    private fun loadConversationalCorpus() {
        // High-frequency English conversational bigrams and trigrams
        // "how are" -> you, things, we
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
        addTrigram("i", "will", "let", 225)

        // "on my" -> way
        addBigram("on", "my", 245)
        addBigram("on", "the", 250)
        addBigram("on", "it", 210)
        addTrigram("on", "my", "way", 255)
        addTrigram("on", "the", "way", 240)
        addTrigram("on", "the", "other", 220)

        // "let me" -> know
        addBigram("let", "me", 250)
        addBigram("let", "us", 200)
        addTrigram("let", "me", "know", 255)
        addTrigram("let", "me", "see", 220)
        addTrigram("let", "me", "check", 215)

        // "see you" -> soon, later, tomorrow
        addBigram("see", "you", 255)
        addTrigram("see", "you", "soon", 245)
        addTrigram("see", "you", "later", 240)
        addTrigram("see", "you", "tomorrow", 235)
        addTrigram("see", "you", "there", 225)

        // "good" -> morning, night, luck, job
        addBigram("good", "morning", 250)
        addBigram("good", "night", 245)
        addBigram("good", "job", 220)
        addBigram("good", "luck", 220)
        addBigram("good", "idea", 215)
        addBigram("good", "to", 210)

        // "sounds" -> good, great, like
        addBigram("sounds", "good", 250)
        addBigram("sounds", "great", 240)
        addBigram("sounds", "like", 230)
        addTrigram("sounds", "like", "a", 245)

        // "what are", "what do", "what is"
        addBigram("what", "are", 245)
        addBigram("what", "do", 240)
        addBigram("what", "is", 240)
        addBigram("what", "time", 225)
        addTrigram("what", "are", "you", 255)
        addTrigram("what", "do", "you", 255)
        addTrigram("what", "is", "the", 245)
        addTrigram("what", "time", "is", 240)

        // "where are", "where is"
        addBigram("where", "are", 250)
        addBigram("where", "is", 240)
        addTrigram("where", "are", "you", 255)

        // "can you", "could you"
        addBigram("can", "you", 255)
        addBigram("can", "we", 230)
        addBigram("can", "i", 240)
        addBigram("could", "you", 245)
        addTrigram("can", "you", "please", 240)
        addTrigram("can", "you", "send", 230)
        addTrigram("can", "you", "call", 225)

        // "are you" -> ok, free, ready
        addBigram("are", "you", 255)
        addTrigram("are", "you", "okay", 240)
        addTrigram("are", "you", "ready", 235)
        addTrigram("are", "you", "free", 230)
        addTrigram("are", "you", "sure", 225)

        // "do you" -> have, know, want
        addBigram("do", "you", 255)
        addBigram("do", "not", 240)
        addTrigram("do", "you", "want", 250)
        addTrigram("do", "you", "have", 245)
        addTrigram("do", "you", "know", 240)
        addTrigram("do", "you", "think", 230)

        // "have a" -> great, good, nice
        addBigram("have", "a", 255)
        addBigram("have", "to", 250)
        addTrigram("have", "a", "great", 245)
        addTrigram("have", "a", "good", 240)
        addTrigram("have", "a", "nice", 235)
        addTrigram("have", "to", "go", 240)

        // "at the"
        addBigram("at", "the", 255)
        addBigram("in", "the", 255)
        addBigram("to", "the", 255)
        addBigram("to", "be", 250)
        addBigram("to", "do", 240)
        addBigram("to", "get", 235)

        // "talk to" -> you later
        addBigram("talk", "to", 250)
        addTrigram("talk", "to", "you", 255)

        // "take care"
        addBigram("take", "care", 250)
        addBigram("take", "your", 230)
        addTrigram("take", "your", "time", 250)

        // "call me" -> when, back
        addBigram("call", "me", 250)
        addTrigram("call", "me", "when", 240)
        addTrigram("call", "me", "back", 245)

        // "keep in" -> touch
        addBigram("keep", "in", 240)
        addTrigram("keep", "in", "touch", 250)

        // "no problem", "no worries"
        addBigram("no", "problem", 250)
        addBigram("no", "worries", 245)

        // "you are" -> welcome, right
        addBigram("you", "are", 250)
        addTrigram("you", "are", "welcome", 255)
        addTrigram("you", "are", "the", 235)
    }
}
