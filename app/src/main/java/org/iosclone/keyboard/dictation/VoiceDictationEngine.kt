package org.iosclone.keyboard.dictation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Intelligent iOS 27 Voice Dictation Engine.
 * Supports spoken punctuation, voice emojis, voice editing commands,
 * and audio RMS wave monitoring for Siri-style live dictation animations.
 */
class VoiceDictationEngine(private val context: Context) {

    private val tag = "VoiceDictationEngine"
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    var onTextRecognized: ((String) -> Unit)? = null
    var onCommandRecognized: ((VoiceCommand) -> Unit)? = null
    var onAudioLevelChanged: ((Float) -> Unit)? = null
    var onDictationStateChanged: ((Boolean) -> Unit)? = null

    enum class VoiceCommand {
        DELETE_LAST_WORD,
        SELECT_ALL,
        CLEAR_ALL,
        NEW_LINE,
        NEW_PARAGRAPH,
        STOP
    }

    private val spokenPunctuation = mapOf(
        "comma" to ",",
        "period" to ".",
        "full stop" to ".",
        "question mark" to "?",
        "exclamation mark" to "!",
        "exclamation point" to "!",
        "colon" to ":",
        "semicolon" to ";",
        "dash" to "-",
        "hyphen" to "-",
        "open quote" to "\"",
        "close quote" to "\""
    )

    private val spokenEmojis = mapOf(
        "smile emoji" to "😊",
        "smiley emoji" to "😊",
        "happy face emoji" to "😊",
        "heart emoji" to "❤️",
        "love emoji" to "❤️",
        "laughing emoji" to "😂",
        "fire emoji" to "🔥",
        "thumbs up emoji" to "👍",
        "thumbs down emoji" to "👎",
        "crying emoji" to "😭",
        "star emoji" to "⭐",
        "party emoji" to "🎉",
        "rocket emoji" to "🚀"
    )

    fun startListening(languageCode: String = "en-US") {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(tag, "Speech recognition not available on device")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Prefer on-device offline recognition when supported
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            onDictationStateChanged?.invoke(true)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognition", e)
            isListening = false
            onDictationStateChanged?.invoke(false)
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
        isListening = false
        onDictationStateChanged?.invoke(false)
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            onAudioLevelChanged?.invoke(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
            onDictationStateChanged?.invoke(false)
        }

        override fun onError(error: Int) {
            isListening = false
            onDictationStateChanged?.invoke(false)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            onDictationStateChanged?.invoke(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: return
            processDictatedSpeech(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: return
            // Live partial stream if desired
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processDictatedSpeech(rawText: String) {
        val lower = rawText.lowercase().trim()

        // 1. Check Voice Editing Commands
        when {
            lower == "stop" || lower == "stop dictation" || lower == "done" -> {
                onCommandRecognized?.invoke(VoiceCommand.STOP)
                return
            }
            lower == "delete word" || lower == "delete last word" -> {
                onCommandRecognized?.invoke(VoiceCommand.DELETE_LAST_WORD)
                return
            }
            lower == "select all" -> {
                onCommandRecognized?.invoke(VoiceCommand.SELECT_ALL)
                return
            }
            lower == "clear all" || lower == "delete all" -> {
                onCommandRecognized?.invoke(VoiceCommand.CLEAR_ALL)
                return
            }
            lower == "new line" -> {
                onCommandRecognized?.invoke(VoiceCommand.NEW_LINE)
                return
            }
            lower == "new paragraph" -> {
                onCommandRecognized?.invoke(VoiceCommand.NEW_PARAGRAPH)
                return
            }
        }

        // 2. Parse Spoken Punctuation and Emojis
        var formatted = rawText

        for ((phrase, punct) in spokenPunctuation) {
            val regex = Regex("\\b$phrase\\b", RegexOption.IGNORE_CASE)
            formatted = formatted.replace(regex, punct)
        }

        for ((phrase, emoji) in spokenEmojis) {
            val regex = Regex("\\b$phrase\\b", RegexOption.IGNORE_CASE)
            formatted = formatted.replace(regex, emoji)
        }

        // Clean up punctuation spacing: "hello , world" -> "hello, world"
        formatted = formatted.replace(Regex("\\s+([,.:;?!])"), "$1")

        onTextRecognized?.invoke(formatted)
    }
}
