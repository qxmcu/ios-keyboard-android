package org.iosclone.keyboard.writingtools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Production Google Gemini API Client for Apple Intelligence Writing Tools.
 * Uses lightweight, battery-efficient HTTP requests to Google's fast Flash-Lite model.
 */
class GeminiWritingToolsClient(private val context: Context) {

    private val tag = "GeminiWritingTools"

    class NoInternetException(message: String = "No internet connection. Please connect to the internet.") : Exception(message)
    class MissingApiKeyException(message: String = "Please set up your Google Gemini API Key first.") : Exception(message)

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val netInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            return netInfo != null && netInfo.isConnected
        }
    }

    suspend fun executeTask(
        apiKey: String,
        model: String,
        task: WritingToolsTask,
        text: String,
        instruction: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(MissingApiKeyException())
        }

        if (!isNetworkAvailable()) {
            return@withContext Result.failure(NoInternetException())
        }

        val prompt = buildPrompt(task, text, instruction)
        val selectedModel = if (model.isNotBlank()) model else "gemini-2.0-flash-lite"

        try {
            val response = callGeminiApi(apiKey, selectedModel, prompt)
            Result.success(cleanResponse(response))
        } catch (e: Exception) {
            // If model is not found, fallback to gemini-1.5-flash
            if (e.message?.contains("404") == true && selectedModel != "gemini-1.5-flash") {
                try {
                    val fallback = callGeminiApi(apiKey, "gemini-1.5-flash", prompt)
                    return@withContext Result.success(cleanResponse(fallback))
                } catch (fallbackEx: Exception) {
                    Log.e(tag, "Gemini fallback failed", fallbackEx)
                }
            }
            Log.e(tag, "Gemini API call failed", e)
            Result.failure(e)
        }
    }

    private fun callGeminiApi(apiKey: String, model: String, prompt: String): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connectTimeout = 12000
            readTimeout = 20000
            doInput = true
            doOutput = true
        }

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.3)
                put("maxOutputTokens", 2048)
            }
            put("generationConfig", genConfig)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestJson.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val root = JSONObject(responseText)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
            return ""
        } else {
            val errorBody = try {
                BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream, "UTF-8")).use { it.readText() }
            } catch (_: Exception) { "" }

            val errorMsg = when {
                responseCode == 400 || errorBody.contains("API_KEY_INVALID") -> "Invalid Gemini API Key. Please verify your key."
                responseCode == 429 || errorBody.contains("RESOURCE_EXHAUSTED") -> "Gemini rate limit exceeded. Please wait a moment."
                responseCode == 404 -> "Model $model not found (HTTP 404)."
                else -> "Gemini request failed (HTTP $responseCode): $errorBody"
            }
            throw RuntimeException(errorMsg)
        }
    }

    private fun buildPrompt(task: WritingToolsTask, text: String, instruction: String): String {
        return when (task) {
            WritingToolsTask.PROOFREAD ->
                "You are Apple Intelligence Writing Tools. Proofread and correct any spelling, grammatical, punctuation, and typographical mistakes in the text below. Preserve the author's tone and exact meaning. Return ONLY the corrected text with no introductions, preamble, or surrounding quotes:\n\n$text"

            WritingToolsTask.REWRITE ->
                "You are Apple Intelligence Writing Tools. Rewrite the following text to improve clarity, flow, and natural phrasing while keeping the core message intact. Return ONLY the rewritten text with no preamble or quotes:\n\n$text"

            WritingToolsTask.FRIENDLY ->
                "You are Apple Intelligence Writing Tools. Rewrite the following text in a warm, friendly, polite, and approachable tone. Return ONLY the rewritten text with no preamble or quotes:\n\n$text"

            WritingToolsTask.PROFESSIONAL ->
                "You are Apple Intelligence Writing Tools. Rewrite the following text in a polished, professional, and business-appropriate tone. Return ONLY the rewritten text with no preamble or quotes:\n\n$text"

            WritingToolsTask.CONCISE ->
                "You are Apple Intelligence Writing Tools. Rewrite the following text to be concise, direct, and succinct, removing filler words while keeping all important information. Return ONLY the rewritten text with no preamble or quotes:\n\n$text"

            WritingToolsTask.SUMMARY ->
                "You are Apple Intelligence Writing Tools. Provide a concise, clear summary of the following text. Return ONLY the summary:\n\n$text"

            WritingToolsTask.KEY_POINTS ->
                "You are Apple Intelligence Writing Tools. Extract the key takeaways and main points from the following text as bullet points (using •). Return ONLY the bullet points:\n\n$text"

            WritingToolsTask.LIST ->
                "You are Apple Intelligence Writing Tools. Format the following text into a clean, well-structured bulleted or numbered list. Return ONLY the list:\n\n$text"

            WritingToolsTask.TABLE ->
                "You are Apple Intelligence Writing Tools. Format the data and information in the following text into a clean markdown table. Return ONLY the table:\n\n$text"

            WritingToolsTask.COMPOSE ->
                "You are Apple Intelligence Writing Tools. Compose a well-written draft based on the following request: $instruction\n\nContext text:\n$text\n\nReturn ONLY the composed text:"

            WritingToolsTask.CUSTOM ->
                "You are Apple Intelligence Writing Tools. Apply the following change to the text:\nChange instruction: $instruction\n\nOriginal text:\n$text\n\nReturn ONLY the modified text with no preamble or quotes:"
        }
    }

    private fun cleanResponse(response: String): String {
        var cleaned = response.trim()
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            val lines = cleaned.lines()
            if (lines.size >= 2) {
                cleaned = lines.subList(1, lines.size - 1).joinToString("\n").trim()
            }
        }
        return cleaned
    }
}

enum class WritingToolsTask {
    PROOFREAD,
    REWRITE,
    FRIENDLY,
    PROFESSIONAL,
    CONCISE,
    SUMMARY,
    KEY_POINTS,
    LIST,
    TABLE,
    COMPOSE,
    CUSTOM
}
