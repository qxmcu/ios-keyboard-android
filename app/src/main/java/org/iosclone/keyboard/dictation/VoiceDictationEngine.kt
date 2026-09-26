package org.iosclone.keyboard.dictation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Intelligent iOS 27 Voice Dictation Engine.
 * Features continuous listening (doesn't close on speech pauses), spoken punctuation parsing,
 * voice emojis, voice editing commands, and Siri-style live audio wave RMS monitoring.
 */
class VoiceDictationEngine(private val context: Context) {

    private val tag = "VoiceDictationEngine"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentLangCode: String = "en-US"

    private val whisperFlow = WhisperFlowEngine(context)

    /**
     * Whether dictation mode is active from the user's perspective.
     * The UI stays open and restarts recognition segments continuously until the user taps Done.
     */
    var isDictationActive = false
        private set

    /**
     * Whether the speech recognizer is actively recording an utterance.
     */
    var isListening = false
        private set

    var onTextRecognized: ((String) -> Unit)? = null
    var onPartialTextRecognized: ((String) -> Unit)? = null
    var onCommandRecognized: ((VoiceCommand) -> Unit)? = null
    var onAudioLevelChanged: ((Float) -> Unit)? = null
    var onDictationStateChanged: ((Boolean) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null
    var onPermissionNeeded: (() -> Unit)? = null

    init {
        whisperFlow.onPartialResult = { text ->
            onPartialTextRecognized?.invoke(text)
            onStatusChanged?.invoke("🎙 Whisper Flow: $text")
        }
        whisperFlow.onFinalResult = { text ->
            processDictatedSpeech(text)
        }
        whisperFlow.onRmsChanged = { rmsDb ->
            onAudioLevelChanged?.invoke(rmsDb)
        }
        whisperFlow.onStatusChanged = { status ->
            onStatusChanged?.invoke(status)
        }
        whisperFlow.onError = { _ ->
            if (isDictationActive && !isListening) {
                startSystemListeningInternal()
            }
        }
    }

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

    /**
     * Checks if the app has runtime RECORD_AUDIO permission.
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts continuous voice dictation session using Whisper Flow.
     */
    fun startListening(languageCode: String = "en-US") {
        currentLangCode = languageCode

        if (!hasAudioPermission()) {
            isDictationActive = false
            onPermissionNeeded?.invoke()
            return
        }

        isDictationActive = true
        onDictationStateChanged?.invoke(true)
        val started = whisperFlow.startListening()
        if (!started) {
            mainHandler.post {
                startSystemListeningInternal()
            }
        }
    }

    fun retryListening() {
        if (!hasAudioPermission()) {
            onPermissionNeeded?.invoke()
            return
        }
        isDictationActive = true
        whisperFlow.stopListening()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        val started = whisperFlow.startListening()
        if (!started) {
            startSystemListeningInternal()
        }
    }

    private fun startSystemListeningInternal() {
        if (!isDictationActive) return
        val appContext = context.applicationContext

        try {
            if (speechRecognizer == null) {
                speechRecognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)) {
                    try {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                    } catch (_: Throwable) {
                        SpeechRecognizer.createSpeechRecognizer(appContext)
                    }
                } else {
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                }
                speechRecognizer?.setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLangCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            onStatusChanged?.invoke("🎙 Whisper Flow (Listening…)")
            onDictationStateChanged?.invoke(true)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognition: ${e.message}", e)
            isListening = false
            onStatusChanged?.invoke("🎙 Tap to speak")
        }
    }

    /**
     * Stops dictation session completely.
     */
    fun stopListening() {
        isDictationActive = false
        isListening = false
        whisperFlow.stopListening()
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        onDictationStateChanged?.invoke(false)
    }

    /**
     * Releases speech resources.
     */
    fun destroy() {
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onStatusChanged?.invoke("🎙 Listening…")
        }

        override fun onBeginningOfSpeech() {
            onStatusChanged?.invoke("🎙 Speaking…")
        }

        override fun onRmsChanged(rmsdB: Float) {
            onAudioLevelChanged?.invoke(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
            onStatusChanged?.invoke("🎙 Processing…")
            // Do NOT close dictation mode on pause! Keep UI alive and waiting for results
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()
            if (!partial.isNullOrBlank()) {
                onPartialTextRecognized?.invoke(partial)
                onStatusChanged?.invoke(partial)
            }
        }

        override fun onError(error: Int) {
            isListening = false
            Log.w(tag, "SpeechRecognizer error: $error")

            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    isDictationActive = false
                    onStatusChanged?.invoke("Microphone permission needed")
                    onPermissionNeeded?.invoke()
                    onDictationStateChanged?.invoke(false)
                }
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal pause or silence: seamlessly restart next listening segment if still in dictation mode
                    if (isDictationActive) {
                        onStatusChanged?.invoke("🎙 Listening…")
                        mainHandler.postDelayed({
                            if (isDictationActive) {
                                startSystemListeningInternal()
                            }
                        }, 250)
                    }
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    try { speechRecognizer?.cancel() } catch (_: Exception) {}
                    if (isDictationActive) {
                        mainHandler.postDelayed({
                            if (isDictationActive) {
                                startSystemListeningInternal()
                            }
                        }, 350)
                    }
                }
                else -> {
                    // For client or audio glitch: retry after brief delay if still active
                    if (isDictationActive) {
                        onStatusChanged?.invoke("🎙 Tap to speak")
                        mainHandler.postDelayed({
                            if (isDictationActive) {
                                startSystemListeningInternal()
                            }
                        }, 500)
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                processDictatedSpeech(text)
            }

            // Continuous dictation: automatically listen for the next sentence!
            if (isDictationActive) {
                mainHandler.postDelayed({
                    if (isDictationActive) {
                        startSystemListeningInternal()
                    }
                }, 150)
            }
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
