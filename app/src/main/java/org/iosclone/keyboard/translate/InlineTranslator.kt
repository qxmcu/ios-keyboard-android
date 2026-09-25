package org.iosclone.keyboard.translate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InlineTranslator(private val engine: TranslationEngine) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var translationJob: Job? = null

    var sourceLang: String = "en"
    var targetLang: String = "es"
    var endpoint: String = TranslationEngine.DEFAULT_ENDPOINT
    var apiKey: String = ""

    var onTranslationResult: ((String) -> Unit)? = null

    fun onTextChanged(text: String) {
        translationJob?.cancel()
        if (text.isBlank()) {
            onTranslationResult?.invoke("")
            return
        }

        translationJob = scope.launch {
            delay(400) // Debounce typing
            val result = engine.translate(
                text = text,
                sourceLang = sourceLang,
                targetLang = targetLang,
                apiEndpoint = endpoint,
                apiKey = apiKey
            )
            onTranslationResult?.invoke(result)
        }
    }
}
