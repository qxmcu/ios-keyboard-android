package org.iosclone.keyboard

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDictionaryBackupHelperTest {

    @Test
    fun testBackupJsonSerialization() {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", 1727380000000L)

        val wordsArray = JSONArray()
        val word1 = JSONObject().apply {
            put("word", "customword")
            put("freq", 120)
            put("lang", "QWERTY")
        }
        wordsArray.put(word1)
        root.put("user_words", wordsArray)

        val bigramsArray = JSONArray()
        val bigram1 = JSONObject().apply {
            put("prev", "good")
            put("next", "morning")
            put("freq", 15)
        }
        bigramsArray.put(bigram1)
        root.put("user_bigrams", bigramsArray)

        val jsonStr = root.toString()
        val parsed = JSONObject(jsonStr)

        assertEquals(1, parsed.getInt("version"))
        val restoredWords = parsed.getJSONArray("user_words")
        assertEquals(1, restoredWords.length())
        assertEquals("customword", restoredWords.getJSONObject(0).getString("word"))

        val restoredBigrams = parsed.getJSONArray("user_bigrams")
        assertEquals(1, restoredBigrams.length())
        assertEquals("good", restoredBigrams.getJSONObject(0).getString("prev"))
        assertEquals("morning", restoredBigrams.getJSONObject(0).getString("next"))
    }
}
