package org.iosclone.keyboard.service

import org.iosclone.keyboard.dictionary.Trie
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.layout.KeyboardMode

interface KeyboardActionListener {
    fun onKey(key: KeyDefinition)
    fun onText(text: String)
    fun onDelete()
    fun onShiftToggle()
    fun onModeChange(mode: KeyboardMode)
    fun onLanguageSwitch()
    fun onLanguageSelected(language: org.iosclone.keyboard.layout.LanguageLayout)
    fun onOneHandedModeChange(oneHandedMode: org.iosclone.keyboard.layout.OneHandedMode)
    fun onEmojiPickerRequested()
    fun onCursorMoved(deltaSteps: Int)
    fun onGlideTypingCompleted(candidates: List<String>)
    fun getDictionaryTrie(): Trie?
}
