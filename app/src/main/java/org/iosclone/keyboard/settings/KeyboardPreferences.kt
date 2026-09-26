package org.iosclone.keyboard.settings

import android.content.Context
import android.content.SharedPreferences
import org.iosclone.keyboard.layout.LanguageLayout
import org.iosclone.keyboard.theme.ThemeMode

class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ios_keyboard_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.SYSTEM }
        }
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var keyboardHeightFactor: Float
        get() = prefs.getFloat("keyboard_height_factor", 1.0f)
        set(value) = prefs.edit().putFloat("keyboard_height_factor", value).apply()

    var bottomSpacingDp: Int
        get() = prefs.getInt("bottom_spacing_dp", 0)
        set(value) = prefs.edit().putInt("bottom_spacing_dp", value).apply()

    var keyPopupsEnabled: Boolean
        get() = prefs.getBoolean("key_popups_enabled", true)
        set(value) = prefs.edit().putBoolean("key_popups_enabled", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var soundVolume: Int
        get() = prefs.getInt("sound_volume", 75)
        set(value) = prefs.edit().putInt("sound_volume", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var hapticsIntensity: Int
        get() = prefs.getInt("haptics_intensity", 65)
        set(value) = prefs.edit().putInt("haptics_intensity", value).apply()

    var autocorrectEnabled: Boolean
        get() = prefs.getBoolean("autocorrect_enabled", true)
        set(value) = prefs.edit().putBoolean("autocorrect_enabled", value).apply()

    var predictiveTextEnabled: Boolean
        get() = prefs.getBoolean("predictive_text_enabled", true)
        set(value) = prefs.edit().putBoolean("predictive_text_enabled", value).apply()

    var gestureTypingEnabled: Boolean
        get() = prefs.getBoolean("gesture_typing_enabled", true)
        set(value) = prefs.edit().putBoolean("gesture_typing_enabled", value).apply()

    var doubleSpacePeriodEnabled: Boolean
        get() = prefs.getBoolean("double_space_period", true)
        set(value) = prefs.edit().putBoolean("double_space_period", value).apply()

    var spacebarTrackpadEnabled: Boolean
        get() = prefs.getBoolean("spacebar_trackpad", true)
        set(value) = prefs.edit().putBoolean("spacebar_trackpad", value).apply()

    var activeLanguage: LanguageLayout
        get() {
            val name = prefs.getString("active_language", LanguageLayout.QWERTY.name) ?: LanguageLayout.QWERTY.name
            return try { LanguageLayout.valueOf(name) } catch (e: Exception) { LanguageLayout.QWERTY }
        }
        set(value) = prefs.edit().putString("active_language", value.name).apply()

    var clipboardHistoryEnabled: Boolean
        get() = prefs.getBoolean("clipboard_history_enabled", true)
        set(value) = prefs.edit().putBoolean("clipboard_history_enabled", value).apply()

    var clipboardAutoSuggest: Boolean
        get() = prefs.getBoolean("clipboard_auto_suggest", true)
        set(value) = prefs.edit().putBoolean("clipboard_auto_suggest", value).apply()

    var translationEnabled: Boolean
        get() = prefs.getBoolean("translation_enabled", false)
        set(value) = prefs.edit().putBoolean("translation_enabled", value).apply()

    var translationTargetLang: String
        get() = prefs.getString("translation_target_lang", "es") ?: "es"
        set(value) = prefs.edit().putString("translation_target_lang", value).apply()

    var translationEndpoint: String
        get() = prefs.getString("translation_endpoint", "https://libretranslate.com/translate") ?: "https://libretranslate.com/translate"
        set(value) = prefs.edit().putString("translation_endpoint", value).apply()

    var writingToolsEnabled: Boolean
        get() = prefs.getBoolean("writing_tools_enabled", true)
        set(value) = prefs.edit().putBoolean("writing_tools_enabled", value).apply()

    var dictationAutoPunctuation: Boolean
        get() = prefs.getBoolean("dictation_auto_punctuation", true)
        set(value) = prefs.edit().putBoolean("dictation_auto_punctuation", value).apply()

    var inlinePredictionsEnabled: Boolean
        get() = prefs.getBoolean("inline_predictions_enabled", true)
        set(value) = prefs.edit().putBoolean("inline_predictions_enabled", value).apply()

    var showNumberRow: Boolean
        get() = prefs.getBoolean("show_number_row", false)
        set(value) = prefs.edit().putBoolean("show_number_row", value).apply()

    var showPeriodKey: Boolean
        get() = prefs.getBoolean("show_period_key", false)
        set(value) = prefs.edit().putBoolean("show_period_key", value).apply()
}
