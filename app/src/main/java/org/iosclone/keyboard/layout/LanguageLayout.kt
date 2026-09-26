package org.iosclone.keyboard.layout

enum class LanguageLayout(val displayName: String, val localeCode: String) {
    QWERTY("English (US)", "en_US"),
    SPANISH("Spanish (Español)", "es_ES"),
    AZERTY("French (Français)", "fr_FR"),
    QWERTZ("German (Deutsch)", "de_DE"),
    CYRILLIC("Russian (Русский)", "ru_RU"),
    ARABIC("Arabic (العربية)", "ar"),
    HINDI("Hindi (हिन्दी)", "hi_IN"),
    CHINESE_PINYIN("Chinese (Pinyin)", "zh_CN"),
    JAPANESE("Japanese (Romaji)", "ja_JP"),
    KOREAN("Korean (한국어)", "ko_KR"),
    THAI("Thai (ไทย)", "th_TH");

    companion object {
        fun fromLocale(code: String): LanguageLayout {
            return entries.firstOrNull { it.localeCode.equals(code, ignoreCase = true) } ?: QWERTY
        }
    }
}
