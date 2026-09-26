package org.iosclone.keyboard.layout

object KeyboardLayoutFactory {

    private val ACCENTS_MAP = mapOf(
        'e' to listOf("è", "é", "ê", "ë", "ē", "ė", "ę"),
        'y' to listOf("ÿ"),
        'u' to listOf("û", "ü", "ù", "ú", "ū"),
        'i' to listOf("î", "ï", "í", "ī", "į", "ì"),
        'o' to listOf("ô", "ö", "ò", "ó", "œ", "ø", "ō", "õ"),
        'a' to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"),
        's' to listOf("ß", "ś", "š"),
        'd' to listOf("ð"),
        'l' to listOf("ł"),
        'z' to listOf("ž", "ź", "ż"),
        'c' to listOf("ç", "ć", "č"),
        'n' to listOf("ñ", "ń"),
        '0' to listOf("°"),
        '$' to listOf("€", "£", "¥", "₩", "₽", "¢"),
        '?' to listOf("¿"),
        '!' to listOf("¡"),
        '"' to listOf("“", "”", "„", "»", "«"),
        '\'' to listOf("‘", "’", "`"),
        '-' to listOf("–", "—", "•"),
        '/' to listOf("\\")
    )

    fun createLayout(
        language: LanguageLayout,
        mode: KeyboardMode,
        oneHandedMode: OneHandedMode = OneHandedMode.NORMAL,
        returnKeyLabel: String = "return"
    ): KeyboardLayout {
        val rows = when (mode) {
            KeyboardMode.NUMERIC -> createNumericRows()
            KeyboardMode.SYMBOL -> createSymbolRows()
            KeyboardMode.LOWERCASE -> createAlphaRows(language, uppercase = false)
            KeyboardMode.UPPERCASE, KeyboardMode.CAPS_LOCK -> createAlphaRows(language, uppercase = true)
        }

        return KeyboardLayout(rows, mode, language).apply {
            this.oneHandedMode = oneHandedMode
            this.returnKeyLabel = returnKeyLabel
        }
    }

    private fun createAlphaRows(language: LanguageLayout, uppercase: Boolean): List<List<KeyDefinition>> {
        val (row1Chars, row2Chars, row3Chars) = when (language) {
            LanguageLayout.QWERTY -> Triple("qwertyuiop", "asdfghjkl", "zxcvbnm")
            LanguageLayout.QWERTZ -> Triple("qwertzuiop", "asdfghjkl", "yxcvbnm")
            LanguageLayout.AZERTY -> Triple("azertyuiop", "qsdfghjklm", "wxcvbn")
            LanguageLayout.SPANISH -> Triple("qwertyuiop", "asdfghjklñ", "zxcvbnm")
            LanguageLayout.CYRILLIC -> Triple("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю")
            LanguageLayout.ARABIC -> Triple("ضصثقفغعهخحج", "شسيبلاتنمكط", "ئءؤرلاىةوزظ")
            LanguageLayout.HINDI -> Triple("ौैाीूबहकदग", "ोे्िुपरकतच", "ंमणनवलशषस")
            LanguageLayout.CHINESE_PINYIN, LanguageLayout.JAPANESE -> Triple("qwertyuiop", "asdfghjkl", "zxcvbnm")
            LanguageLayout.KOREAN -> Triple("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ")
            LanguageLayout.THAI -> Triple("ๆไำพะัีรนยบล", "ฟหกดเ้่าสวง", "ผปแอิืทมใฝ")
        }

        val row1 = row1Chars.map { c ->
            val charStr = if (uppercase) c.uppercase() else c.toString()
            val accents = ACCENTS_MAP[c.lowercaseChar()]?.map { if (uppercase) it.uppercase() else it } ?: emptyList()
            KeyDefinition(
                code = charStr[0].code,
                label = charStr,
                keyType = KeyType.CHARACTER,
                weight = 1.0f,
                accents = accents
            )
        }

        val row2 = row2Chars.map { c ->
            val charStr = if (uppercase) c.uppercase() else c.toString()
            val accents = ACCENTS_MAP[c.lowercaseChar()]?.map { if (uppercase) it.uppercase() else it } ?: emptyList()
            KeyDefinition(
                code = charStr[0].code,
                label = charStr,
                keyType = KeyType.CHARACTER,
                weight = 1.0f,
                accents = accents
            )
        }

        val row3Keys = mutableListOf<KeyDefinition>()
        // Shift Key (left)
        row3Keys.add(
            KeyDefinition(
                code = -1,
                label = "⇧",
                keyType = KeyType.SHIFT,
                weight = 1.4f
            )
        )

        for (c in row3Chars) {
            val charStr = if (uppercase) c.uppercase() else c.toString()
            val accents = ACCENTS_MAP[c.lowercaseChar()]?.map { if (uppercase) it.uppercase() else it } ?: emptyList()
            row3Keys.add(
                KeyDefinition(
                    code = charStr[0].code,
                    label = charStr,
                    keyType = KeyType.CHARACTER,
                    weight = 1.0f,
                    accents = accents
                )
            )
        }

        // Delete Key (right)
        row3Keys.add(
            KeyDefinition(
                code = -5,
                label = "⌫",
                keyType = KeyType.DELETE,
                weight = 1.4f
            )
        )

        // Row 4 (Bottom Bar: 123, Emoji, Space, Return)
        val row4Keys = listOf(
            KeyDefinition(code = -2, label = "123", keyType = KeyType.SWITCH_NUMERIC, weight = 1.35f),
            KeyDefinition(code = -7, label = "🙂", keyType = KeyType.EMOJI, weight = 1.15f),
            KeyDefinition(code = 32, label = "space", keyType = KeyType.SPACE, weight = 5.0f),
            KeyDefinition(code = -4, label = "↵", keyType = KeyType.RETURN, weight = 1.85f)
        )

        return listOf(row1, row2, row3Keys, row4Keys)
    }

    private fun createNumericRows(): List<List<KeyDefinition>> {
        val row1 = "1234567890".map { c ->
            val accents = ACCENTS_MAP[c] ?: emptyList()
            KeyDefinition(code = c.code, label = c.toString(), keyType = KeyType.CHARACTER, weight = 1.0f, accents = accents)
        }

        val row2Chars = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
        val row2 = row2Chars.map { s ->
            val accents = ACCENTS_MAP[s[0]] ?: emptyList()
            KeyDefinition(code = s[0].code, label = s, keyType = KeyType.CHARACTER, weight = 1.0f, accents = accents)
        }

        val row3Keys = mutableListOf<KeyDefinition>()
        row3Keys.add(KeyDefinition(code = -3, label = "#+=", keyType = KeyType.SWITCH_SYMBOL, weight = 1.4f))

        val middleChars = listOf(".", ",", "?", "!", "'")
        for (s in middleChars) {
            val accents = ACCENTS_MAP[s[0]] ?: emptyList()
            row3Keys.add(KeyDefinition(code = s[0].code, label = s, keyType = KeyType.CHARACTER, weight = 1.0f, accents = accents))
        }

        row3Keys.add(KeyDefinition(code = -5, label = "⌫", keyType = KeyType.DELETE, weight = 1.4f))

        val row4Keys = listOf(
            KeyDefinition(code = -1, label = "ABC", keyType = KeyType.SWITCH_ALPHA, weight = 1.35f),
            KeyDefinition(code = -7, label = "🙂", keyType = KeyType.EMOJI, weight = 1.15f),
            KeyDefinition(code = 32, label = "space", keyType = KeyType.SPACE, weight = 5.0f),
            KeyDefinition(code = -4, label = "↵", keyType = KeyType.RETURN, weight = 1.85f)
        )

        return listOf(row1, row2, row3Keys, row4Keys)
    }

    private fun createSymbolRows(): List<List<KeyDefinition>> {
        val row1Chars = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
        val row1 = row1Chars.map { s ->
            KeyDefinition(code = s[0].code, label = s, keyType = KeyType.CHARACTER, weight = 1.0f)
        }

        val row2Chars = listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
        val row2 = row2Chars.map { s ->
            KeyDefinition(code = s[0].code, label = s, keyType = KeyType.CHARACTER, weight = 1.0f)
        }

        val row3Keys = mutableListOf<KeyDefinition>()
        row3Keys.add(KeyDefinition(code = -2, label = "123", keyType = KeyType.SWITCH_NUMERIC, weight = 1.4f))

        val middleChars = listOf(".", ",", "?", "!", "'")
        for (s in middleChars) {
            val accents = ACCENTS_MAP[s[0]] ?: emptyList()
            row3Keys.add(KeyDefinition(code = s[0].code, label = s, keyType = KeyType.CHARACTER, weight = 1.0f, accents = accents))
        }

        row3Keys.add(KeyDefinition(code = -5, label = "⌫", keyType = KeyType.DELETE, weight = 1.4f))

        val row4Keys = listOf(
            KeyDefinition(code = -1, label = "ABC", keyType = KeyType.SWITCH_ALPHA, weight = 1.35f),
            KeyDefinition(code = -7, label = "🙂", keyType = KeyType.EMOJI, weight = 1.15f),
            KeyDefinition(code = 32, label = "space", keyType = KeyType.SPACE, weight = 5.0f),
            KeyDefinition(code = -4, label = "↵", keyType = KeyType.RETURN, weight = 1.85f)
        )

        return listOf(row1, row2, row3Keys, row4Keys)
    }
}
