package org.iosclone.keyboard.translate

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TranslationEngine {

    companion object {
        const val DEFAULT_ENDPOINT = "https://libretranslate.com/translate"
    }

    // Curated offline quick translations for common phrases
    private val offlineDictionary = mapOf(
        "hello" to mapOf("es" to "hola", "fr" to "bonjour", "de" to "hallo", "ru" to "привет", "ar" to "مرحبا"),
        "hi" to mapOf("es" to "hola", "fr" to "salut", "de" to "hallo", "ru" to "привет", "ar" to "أهلا"),
        "thank you" to mapOf("es" to "gracias", "fr" to "merci", "de" to "danke", "ru" to "спасибо", "ar" to "شكرا"),
        "thanks" to mapOf("es" to "gracias", "fr" to "merci", "de" to "danke", "ru" to "спасибо", "ar" to "شكرا"),
        "how are you" to mapOf("es" to "¿cómo estás?", "fr" to "comment vas-tu?", "de" to "wie geht's?", "ru" to "как дела?", "ar" to "كيف حالك؟"),
        "good morning" to mapOf("es" to "buenos días", "fr" to "bonjour", "de" to "guten morgen", "ru" to "доброе утро", "ar" to "صباح الخير"),
        "good night" to mapOf("es" to "buenas noches", "fr" to "bonne nuit", "de" to "gute nacht", "ru" to "спокойной ночи", "ar" to "تصبح على خير"),
        "yes" to mapOf("es" to "sí", "fr" to "oui", "de" to "ja", "ru" to "да", "ar" to "نعم"),
        "no" to mapOf("es" to "no", "fr" to "non", "de" to "nein", "ru" to "нет", "ar" to "لا"),
        "please" to mapOf("es" to "por favor", "fr" to "s'il vous plaît", "de" to "bitte", "ru" to "пожалуйста", "ar" to "من فضلك"),
        "goodbye" to mapOf("es" to "adiós", "fr" to "au revoir", "de" to "auf wiedersehen", "ru" to "до свидания", "ar" to "مع السلامة"),
        "i love you" to mapOf("es" to "te quiero", "fr" to "je t'aime", "de" to "ich liebe dich", "ru" to "я люблю тебя", "ar" to "أحبك")
    )

    suspend fun translate(
        text: String,
        sourceLang: String = "en",
        targetLang: String = "es",
        apiEndpoint: String = DEFAULT_ENDPOINT,
        apiKey: String = ""
    ): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext ""

        // 1. Check offline phrases first
        val offlineHit = offlineDictionary[trimmed.lowercase()]?.get(targetLang.lowercase())
        if (offlineHit != null) {
            return@withContext offlineHit
        }

        // 2. Query LibreTranslate endpoint if reachable
        try {
            val url = URL(apiEndpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("q", trimmed)
                put("source", sourceLang)
                put("target", targetLang)
                put("format", "text")
                if (apiKey.isNotEmpty()) {
                    put("api_key", apiKey)
                }
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                return@withContext jsonResponse.optString("translatedText", trimmed)
            }
        } catch (e: Exception) {
            Log.w("TranslationEngine", "Online translation failed: ${e.message}")
        }

        return@withContext trimmed
    }
}
