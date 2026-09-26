package org.iosclone.keyboard.service

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.iosclone.keyboard.audio.AudioHapticFeedback
import org.iosclone.keyboard.audio.SoundType
import org.iosclone.keyboard.clipboard.ClipboardManagerHelper
import org.iosclone.keyboard.dictation.DictationOverlayView
import org.iosclone.keyboard.dictation.VoiceDictationEngine
import org.iosclone.keyboard.dictionary.DictionaryEngine
import org.iosclone.keyboard.dictionary.Trie
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.layout.KeyType
import org.iosclone.keyboard.layout.KeyboardLayout
import org.iosclone.keyboard.layout.KeyboardLayoutFactory
import org.iosclone.keyboard.layout.KeyboardMode
import org.iosclone.keyboard.layout.LanguageLayout
import org.iosclone.keyboard.layout.OneHandedMode
import org.iosclone.keyboard.settings.KeyboardPreferences
import org.iosclone.keyboard.settings.SettingsActivity
import org.iosclone.keyboard.theme.KeyboardTheme
import org.iosclone.keyboard.theme.ThemeColors
import org.iosclone.keyboard.translate.InlineTranslator
import org.iosclone.keyboard.translate.TranslationEngine
import org.iosclone.keyboard.view.ClipboardDrawerView
import org.iosclone.keyboard.view.EmojiPickerView
import org.iosclone.keyboard.view.IOSKeyboardView
import org.iosclone.keyboard.view.SuggestionStripView
import org.iosclone.keyboard.view.TranslationBarView
import org.iosclone.keyboard.writingtools.WritingToolsBottomSheet

class IOSInputMethodService : InputMethodService(), KeyboardActionListener {

    private lateinit var preferences: KeyboardPreferences
    private lateinit var themeResolver: KeyboardTheme
    private lateinit var audioHapticFeedback: AudioHapticFeedback
    private lateinit var dictionaryEngine: DictionaryEngine
    private lateinit var clipboardHelper: ClipboardManagerHelper
    private lateinit var translationEngine: TranslationEngine
    private lateinit var inlineTranslator: InlineTranslator
    private lateinit var voiceDictationEngine: VoiceDictationEngine

    // Views
    private var rootLayout: LinearLayout? = null
    private var suggestionStripView: SuggestionStripView? = null
    private var translationBarView: TranslationBarView? = null
    private var contentContainer: FrameLayout? = null
    private var keyboardView: IOSKeyboardView? = null
    private var emojiPickerView: EmojiPickerView? = null
    private var clipboardDrawerView: ClipboardDrawerView? = null
    private var dictationOverlayView: DictationOverlayView? = null

    // State
    private var currentMode = KeyboardMode.LOWERCASE
    private var currentLanguage = LanguageLayout.QWERTY
    private var oneHandedMode = OneHandedMode.NORMAL
    private var currentTheme: ThemeColors = ThemeColors.Light

    private val wordBuffer = StringBuilder()
    private var lastShiftPressTime = 0L
    private var lastSpacePressTime = 0L
    private var returnActionId = EditorInfo.IME_ACTION_NONE
    private var returnActionLabel = "return"

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        themeResolver = KeyboardTheme(this)
        audioHapticFeedback = AudioHapticFeedback(this)
        dictionaryEngine = DictionaryEngine(this)
        clipboardHelper = ClipboardManagerHelper(this)
        translationEngine = TranslationEngine()
        inlineTranslator = InlineTranslator(translationEngine)
        voiceDictationEngine = VoiceDictationEngine(this)

        currentLanguage = preferences.activeLanguage
        dictionaryEngine.setLanguage(currentLanguage)

        clipboardHelper.startListening()
        clipboardHelper.onNewClipAvailable = { clip ->
            Handler(Looper.getMainLooper()).post {
                if (preferences.clipboardAutoSuggest) {
                    suggestionStripView?.showQuickPaste(clip)
                }
            }
        }

        // Voice Dictation callbacks
        voiceDictationEngine.onTextRecognized = { spokenText ->
            currentInputConnection?.commitText(spokenText + " ", 1)
            updateNextWordPredictions()
        }
        voiceDictationEngine.onCommandRecognized = { command ->
            when (command) {
                VoiceDictationEngine.VoiceCommand.DELETE_LAST_WORD -> {
                    val ic = currentInputConnection
                    val before = ic?.getTextBeforeCursor(50, 0)?.toString() ?: ""
                    val lastWord = before.trimEnd().substringAfterLast(' ', "")
                    if (lastWord.isNotEmpty()) {
                        ic?.deleteSurroundingText(lastWord.length + 1, 0)
                    }
                }
                VoiceDictationEngine.VoiceCommand.SELECT_ALL -> {
                    currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
                }
                VoiceDictationEngine.VoiceCommand.CLEAR_ALL -> {
                    currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
                    currentInputConnection?.commitText("", 1)
                }
                VoiceDictationEngine.VoiceCommand.NEW_LINE -> {
                    currentInputConnection?.commitText("\n", 1)
                }
                VoiceDictationEngine.VoiceCommand.NEW_PARAGRAPH -> {
                    currentInputConnection?.commitText("\n\n", 1)
                }
                VoiceDictationEngine.VoiceCommand.STOP -> {
                    dictationOverlayView?.visibility = View.GONE
                    keyboardView?.visibility = View.VISIBLE
                }
            }
        }
        voiceDictationEngine.onAudioLevelChanged = { rms ->
            dictationOverlayView?.setAudioLevel(rms)
        }
        voiceDictationEngine.onDictationStateChanged = { listening ->
            if (!listening) {
                dictationOverlayView?.visibility = View.GONE
                keyboardView?.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateInputView(): View {
        currentTheme = themeResolver.resolveTheme(preferences.themeMode)

        val density = resources.displayMetrics.density
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(currentTheme.keyboardBackground)
        }

        // 1. Suggestion Strip
        suggestionStripView = SuggestionStripView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * density).toInt()
            )
            onCandidateSelected = { candidate -> commitCandidate(candidate) }
            onQuickPasteSelected = { pasteText ->
                currentInputConnection?.commitText(pasteText, 1)
                showQuickPaste(null)
            }
            onWritingToolsRequested = {
                val anchor = rootLayout ?: this
                val textBefore = currentInputConnection?.getTextBeforeCursor(300, 0)?.toString() ?: ""
                val selectedText = currentInputConnection?.getSelectedText(0)?.toString() ?: ""
                val textToProcess = if (selectedText.isNotBlank()) selectedText else textBefore.takeLast(160).trim()

                WritingToolsBottomSheet(
                    context = this@IOSInputMethodService,
                    theme = currentTheme,
                    currentText = textToProcess
                ) { replacement ->
                    if (selectedText.isNotBlank()) {
                        currentInputConnection?.commitText(replacement, 1)
                    } else if (textToProcess.isNotEmpty()) {
                        currentInputConnection?.deleteSurroundingText(textToProcess.length, 0)
                        currentInputConnection?.commitText(replacement, 1)
                    }
                    wordBuffer.clear()
                    updateNextWordPredictions()
                }.show(anchor)
            }
        }
        rootLayout?.addView(suggestionStripView)

        // 2. Translation Bar (collapsible)
        translationBarView = TranslationBarView(this).apply {
            visibility = View.GONE
            setLanguages(inlineTranslator.sourceLang, inlineTranslator.targetLang)
            onSwapLanguages = {
                val temp = inlineTranslator.sourceLang
                inlineTranslator.sourceLang = inlineTranslator.targetLang
                inlineTranslator.targetLang = temp
                setLanguages(inlineTranslator.sourceLang, inlineTranslator.targetLang)
                inlineTranslator.onTextChanged(wordBuffer.toString())
            }
            onInsertClicked = { previewText ->
                if (previewText.isNotEmpty()) {
                    currentInputConnection?.commitText(previewText, 1)
                }
            }
            onCloseClicked = {
                visibility = View.GONE
            }
        }
        rootLayout?.addView(translationBarView)

        inlineTranslator.onTranslationResult = { translated ->
            translationBarView?.setTranslationPreview(translated)
        }

        // 3. Content Container (Switches between Main Keyboard, Emoji, Clipboard, Dictation)
        contentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 4. Main Keyboard View
        keyboardView = IOSKeyboardView(this).apply {
            actionListener = this@IOSInputMethodService
            audioHapticFeedback = this@IOSInputMethodService.audioHapticFeedback
            preferences = this@IOSInputMethodService.preferences
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        contentContainer?.addView(keyboardView)

        // 5. Emoji Picker View
        emojiPickerView = EmojiPickerView(this).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (260 * density).toInt()
            )
            onEmojiSelected = { emoji ->
                currentInputConnection?.commitText(emoji, 1)
            }
            onBackspaceClicked = {
                onDelete()
            }
            onBackToAlphaClicked = {
                showMainKeyboard()
            }
            onGlobeClicked = {
                showMainKeyboard()
                keyboardView?.showLanguageContextMenu()
            }
            onDictationClicked = {
                onKey(KeyDefinition(code = -11, label = "🎙", keyType = KeyType.DICTATION, weight = 1.0f))
            }
        }
        contentContainer?.addView(emojiPickerView)

        // 6. Clipboard Drawer View
        clipboardDrawerView = ClipboardDrawerView(this).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (260 * density).toInt()
            )
            bindHelper(clipboardHelper)
            onClipSelected = { text ->
                currentInputConnection?.commitText(text, 1)
                showMainKeyboard()
            }
            onCloseRequested = {
                showMainKeyboard()
            }
        }
        contentContainer?.addView(clipboardDrawerView)

        // 7. Dictation Overlay View
        dictationOverlayView = DictationOverlayView(this).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (240 * density).toInt()
            )
            onDoneClicked = {
                voiceDictationEngine.stopListening()
                visibility = View.GONE
                keyboardView?.visibility = View.VISIBLE
            }
        }
        contentContainer?.addView(dictationOverlayView)

        rootLayout?.addView(contentContainer)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout!!) { _, insets ->
            applyBottomInsets(insets)
            insets
        }

        rootLayout?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                ViewCompat.requestApplyInsets(v)
                ViewCompat.getRootWindowInsets(v)?.let { insets ->
                    applyBottomInsets(insets)
                }
            }
            override fun onViewDetachedFromWindow(v: View) {}
        })

        window?.window?.decorView?.let { decor ->
            ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                applyBottomInsets(insets)
                insets
            }
        }

        updateKeyboardLayout()
        applyCurrentTheme()

        return rootLayout!!
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        wordBuffer.clear()
        suggestionStripView?.clearSuggestions()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        rootLayout?.let { root ->
            ViewCompat.getRootWindowInsets(root)?.let { insets ->
                applyBottomInsets(insets)
            }
        }

        // Determine return key label & action
        val imeAction = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        returnActionId = imeAction
        returnActionLabel = when (imeAction) {
            EditorInfo.IME_ACTION_GO -> "go"
            EditorInfo.IME_ACTION_SEARCH -> "search"
            EditorInfo.IME_ACTION_SEND -> "send"
            EditorInfo.IME_ACTION_NEXT -> "next"
            EditorInfo.IME_ACTION_DONE -> "done"
            else -> "return"
        }

        // Auto capitalization
        val inputType = info?.inputType ?: 0
        val isCapSentences = (inputType and EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
        val isCapWords = (inputType and EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS) != 0
        val isCapCharacters = (inputType and EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0

        currentMode = when {
            isCapCharacters -> KeyboardMode.CAPS_LOCK
            isCapSentences || isCapWords -> KeyboardMode.UPPERCASE
            else -> KeyboardMode.LOWERCASE
        }

        currentTheme = themeResolver.resolveTheme(preferences.themeMode)
        applyCurrentTheme()
        updateKeyboardLayout()
        showMainKeyboard()
        updateNextWordPredictions()
    }

    private fun applyBottomInsets(insets: WindowInsetsCompat) {
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val tappable = insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom
        val mandatory = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures()).bottom
        val bottomInset = maxOf(navBars, tappable, mandatory)

        keyboardView?.setBottomInset(bottomInset)

        val density = resources.displayMetrics.density
        val pickerBaseH = (260 * density).toInt()
        emojiPickerView?.let { picker ->
            picker.layoutParams = (picker.layoutParams as? FrameLayout.LayoutParams)?.apply {
                height = pickerBaseH + bottomInset
            } ?: FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, pickerBaseH + bottomInset)
            picker.setPadding(0, 0, 0, bottomInset)
            picker.requestLayout()
        }

        clipboardDrawerView?.let { drawer ->
            drawer.layoutParams = (drawer.layoutParams as? FrameLayout.LayoutParams)?.apply {
                height = pickerBaseH + bottomInset
            } ?: FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, pickerBaseH + bottomInset)
            drawer.setPadding(0, 0, 0, bottomInset)
            drawer.requestLayout()
        }

        dictationOverlayView?.let { overlay ->
            overlay.layoutParams = (overlay.layoutParams as? FrameLayout.LayoutParams)?.apply {
                height = (240 * density).toInt() + bottomInset
            } ?: FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (240 * density).toInt() + bottomInset)
            overlay.setPadding(0, 0, 0, bottomInset)
            overlay.requestLayout()
        }
    }

    private fun applyCurrentTheme() {
        rootLayout?.setBackgroundColor(currentTheme.keyboardBackground)
        keyboardView?.applyTheme(currentTheme)
        suggestionStripView?.applyTheme(currentTheme)
        translationBarView?.applyTheme(currentTheme)
        emojiPickerView?.applyTheme(currentTheme)
        clipboardDrawerView?.applyTheme(currentTheme)
        dictationOverlayView?.applyTheme(currentTheme)
    }

    private fun updateKeyboardLayout() {
        val layout = KeyboardLayoutFactory.createLayout(
            language = currentLanguage,
            mode = currentMode,
            oneHandedMode = oneHandedMode,
            returnKeyLabel = returnActionLabel
        )
        keyboardView?.layout = layout
    }

    private fun showMainKeyboard() {
        keyboardView?.visibility = View.VISIBLE
        emojiPickerView?.visibility = View.GONE
        clipboardDrawerView?.visibility = View.GONE
        dictationOverlayView?.visibility = View.GONE
        suggestionStripView?.visibility = View.VISIBLE
    }

    private fun showEmojiPicker() {
        keyboardView?.visibility = View.GONE
        clipboardDrawerView?.visibility = View.GONE
        dictationOverlayView?.visibility = View.GONE
        emojiPickerView?.visibility = View.VISIBLE
    }

    private fun showClipboardDrawer() {
        keyboardView?.visibility = View.GONE
        emojiPickerView?.visibility = View.GONE
        dictationOverlayView?.visibility = View.GONE
        clipboardDrawerView?.refreshClips()
        clipboardDrawerView?.visibility = View.VISIBLE
    }

    // KeyboardActionListener Callbacks

    override fun onKey(key: KeyDefinition) {
        val ic = currentInputConnection ?: return

        when (key.keyType) {
            KeyType.SPACE -> {
                val now = System.currentTimeMillis()
                // Double tap space for period
                if (preferences.doubleSpacePeriodEnabled && (now - lastSpacePressTime < 350) && wordBuffer.isEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(". ", 1)
                    lastSpacePressTime = 0
                    currentMode = KeyboardMode.UPPERCASE
                    updateKeyboardLayout()
                    updateNextWordPredictions()
                    return
                }
                lastSpacePressTime = now

                val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
                val previousWords = extractPreviousWords(textBefore)

                // Check Autocorrect on space
                if (preferences.autocorrectEnabled && wordBuffer.isNotEmpty()) {
                    val typed = wordBuffer.toString()
                    val result = dictionaryEngine.getSuggestions(typed, previousWords, true)
                    if (result.isAutocorrectCandidate && result.centerCandidate.isNotEmpty() && !result.centerCandidate.equals(typed, ignoreCase = true)) {
                        // Replace misspelled word with autocorrect candidate!
                        ic.deleteSurroundingText(typed.length, 0)
                        ic.commitText(result.centerCandidate + " ", 1)
                        dictionaryEngine.undoManager.recordReplacement(typed, result.centerCandidate)
                        dictionaryEngine.onWordCommitted(result.centerCandidate, previousWords.lastOrNull())
                        wordBuffer.clear()
                        updateNextWordPredictions()
                        return
                    }
                }

                if (wordBuffer.isNotEmpty()) {
                    val typed = wordBuffer.toString()
                    dictionaryEngine.onWordCommitted(typed, previousWords.lastOrNull())
                }
                ic.commitText(" ", 1)
                wordBuffer.clear()
                updateNextWordPredictions()
            }

            KeyType.RETURN -> {
                if (returnActionId != EditorInfo.IME_ACTION_NONE && returnActionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(returnActionId)
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
                wordBuffer.clear()
                suggestionStripView?.clearSuggestions()
            }

            KeyType.DICTATION -> {
                if (voiceDictationEngine.isListening) {
                    voiceDictationEngine.stopListening()
                    dictationOverlayView?.visibility = View.GONE
                    keyboardView?.visibility = View.VISIBLE
                } else {
                    keyboardView?.visibility = View.GONE
                    emojiPickerView?.visibility = View.GONE
                    clipboardDrawerView?.visibility = View.GONE
                    dictationOverlayView?.visibility = View.VISIBLE
                    voiceDictationEngine.startListening(currentLanguage.localeCode)
                }
            }

            else -> {}
        }
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)

        if (text.length == 1 && (text[0].isLetter() || text[0] == '\'')) {
            wordBuffer.append(text)
            updateSuggestions()
            inlineTranslator.onTextChanged(wordBuffer.toString())

            // Revert uppercase back to lowercase after typing a letter (unless CAPS_LOCK)
            if (currentMode == KeyboardMode.UPPERCASE) {
                currentMode = KeyboardMode.LOWERCASE
                updateKeyboardLayout()
            }
        } else {
            wordBuffer.clear()
            updateNextWordPredictions()
        }
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""

        // Signature iOS feature: Undo Autocorrect on immediate backspace!
        val undoRecord = dictionaryEngine.undoManager.shouldUndoOnDelete(textBefore)
        if (undoRecord != null) {
            val replacedWithSpace = undoRecord.autocorrectedTo + " "
            val len = if (textBefore.endsWith(replacedWithSpace)) replacedWithSpace.length else undoRecord.autocorrectedTo.length
            ic.deleteSurroundingText(len, 0)
            ic.commitText(undoRecord.originalTyped, 1)
            wordBuffer.clear()
            wordBuffer.append(undoRecord.originalTyped)
            updateSuggestions()
            return
        }

        ic.deleteSurroundingText(1, 0)

        if (wordBuffer.isNotEmpty()) {
            wordBuffer.deleteCharAt(wordBuffer.length - 1)
            updateSuggestions()
            inlineTranslator.onTextChanged(wordBuffer.toString())
        } else {
            updateNextWordPredictions()
        }
    }

    override fun onShiftToggle() {
        val now = System.currentTimeMillis()
        currentMode = when (currentMode) {
            KeyboardMode.LOWERCASE -> KeyboardMode.UPPERCASE
            KeyboardMode.UPPERCASE -> {
                if (now - lastShiftPressTime < 300) {
                    KeyboardMode.CAPS_LOCK
                } else {
                    KeyboardMode.LOWERCASE
                }
            }
            KeyboardMode.CAPS_LOCK -> KeyboardMode.LOWERCASE
            else -> KeyboardMode.LOWERCASE
        }
        lastShiftPressTime = now
        updateKeyboardLayout()
    }

    override fun onModeChange(mode: KeyboardMode) {
        currentMode = mode
        updateKeyboardLayout()
    }

    override fun onLanguageSwitch() {
        keyboardView?.showLanguageContextMenu()
    }

    override fun onLanguageSelected(language: LanguageLayout) {
        currentLanguage = language
        preferences.activeLanguage = currentLanguage
        dictionaryEngine.setLanguage(currentLanguage)
        updateKeyboardLayout()
    }

    override fun onOneHandedModeChange(oneHandedMode: OneHandedMode) {
        this.oneHandedMode = oneHandedMode
        updateKeyboardLayout()
    }

    override fun onEmojiPickerRequested() {
        showEmojiPicker()
    }

    override fun onCursorMoved(deltaSteps: Int) {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(100, 0) ?: ""
        val textAfter = ic.getTextAfterCursor(100, 0) ?: ""
        val currentPos = textBefore.length
        val targetPos = (currentPos + deltaSteps).coerceIn(0, currentPos + textAfter.length)

        if (targetPos != currentPos) {
            ic.setSelection(targetPos, targetPos)
            audioHapticFeedback.performHapticFeedback(SoundType.STANDARD, preferences.hapticsEnabled, preferences.hapticsIntensity)
        }
    }

    override fun onGlideTypingCompleted(candidates: List<String>) {
        if (candidates.isEmpty()) return
        val topWord = candidates.first()
        val ic = currentInputConnection ?: return
        ic.commitText(topWord + " ", 1)
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val previousWords = extractPreviousWords(textBefore)
        dictionaryEngine.onWordCommitted(topWord, previousWords.dropLast(1).lastOrNull())
        wordBuffer.clear()
        updateNextWordPredictions()
    }

    override fun getDictionaryTrie(): Trie? {
        return dictionaryEngine.getTrie(currentLanguage)
    }

    private fun commitCandidate(candidate: String) {
        val ic = currentInputConnection ?: return
        if (wordBuffer.isNotEmpty()) {
            ic.deleteSurroundingText(wordBuffer.length, 0)
        }
        ic.commitText(candidate + " ", 1)
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val previousWords = extractPreviousWords(textBefore)
        dictionaryEngine.onWordCommitted(candidate, previousWords.dropLast(1).lastOrNull())
        wordBuffer.clear()
        updateNextWordPredictions()
    }

    private fun updateSuggestions() {
        if (!preferences.predictiveTextEnabled || wordBuffer.isEmpty()) {
            updateNextWordPredictions()
            return
        }
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val previousWords = extractPreviousWords(textBefore)
        val result = dictionaryEngine.getSuggestions(
            rawInput = wordBuffer.toString(),
            previousWords = previousWords,
            autocorrectEnabled = preferences.autocorrectEnabled
        )
        suggestionStripView?.setSuggestions(result)
    }

    private fun updateNextWordPredictions() {
        if (!preferences.predictiveTextEnabled) {
            suggestionStripView?.clearSuggestions()
            return
        }
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val previousWords = extractPreviousWords(textBefore)
        if (previousWords.isNotEmpty()) {
            val nextResult = dictionaryEngine.getSuggestions(
                rawInput = "",
                previousWords = previousWords,
                autocorrectEnabled = preferences.autocorrectEnabled
            )
            suggestionStripView?.setSuggestions(nextResult)
        } else {
            suggestionStripView?.clearSuggestions()
        }
    }

    private fun extractPreviousWords(text: String): List<String> {
        return text.split(Regex("[^a-zA-Z'0-9]+"))
            .filter { it.isNotBlank() }
            .takeLast(4)
    }

    override fun onDestroy() {
        clipboardHelper.stopListening()
        voiceDictationEngine.destroy()
        audioHapticFeedback.release()
        super.onDestroy()
    }
}
