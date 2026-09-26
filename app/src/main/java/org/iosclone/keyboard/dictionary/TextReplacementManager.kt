package org.iosclone.keyboard.dictionary

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Manages iOS-style text replacement shortcuts (e.g. "omw" -> "On my way!").
 * Synchronously checks and expands shortcuts upon typing space or punctuation.
 */
class TextReplacementManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ios_text_replacements", Context.MODE_PRIVATE)

    private val defaultShortcuts = mapOf(
        "omw" to "On my way!",
        "brb" to "Be right back!",
        "ty" to "Thank you!",
        "np" to "No problem!",
        "idk" to "I don't know",
        "imo" to "In my opinion",
        "tbh" to "To be honest",
        "btw" to "By the way",
        "tysm" to "Thank you so much!",
        "lmk" to "Let me know",
        "ily" to "I love you"
    )

    private val userShortcuts = mutableMapOf<String, String>()

    init {
        loadShortcuts()
    }

    private fun loadShortcuts() {
        userShortcuts.clear()
        userShortcuts.putAll(defaultShortcuts)

        val jsonStr = prefs.getString("shortcuts_json", null)
        if (jsonStr != null) {
            try {
                val json = JSONObject(jsonStr)
                for (key in json.keys()) {
                    userShortcuts[key] = json.getString(key)
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    private fun saveShortcuts() {
        try {
            val json = JSONObject()
            for ((k, v) in userShortcuts) {
                json.put(k, v)
            }
            prefs.edit().putString("shortcuts_json", json.toString()).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Checks if a word is an active shortcut. Returns replacement text or null.
     */
    fun getReplacement(shortcut: String): String? {
        val lower = shortcut.trim().lowercase()
        return userShortcuts[lower]
    }

    fun getAllShortcuts(): Map<String, String> {
        return userShortcuts.toMap()
    }

    fun addOrUpdateShortcut(shortcut: String, phrase: String) {
        val s = shortcut.trim().lowercase()
        val p = phrase.trim()
        if (s.isNotEmpty() && p.isNotEmpty()) {
            userShortcuts[s] = p
            saveShortcuts()
        }
    }

    fun deleteShortcut(shortcut: String) {
        val s = shortcut.trim().lowercase()
        userShortcuts.remove(s)
        saveShortcuts()
    }
}
