package org.iosclone.keyboard.theme

import android.content.Context
import android.content.res.Configuration

class KeyboardTheme(private val context: Context) {
    
    fun resolveTheme(mode: ThemeMode): ThemeColors {
        return when (mode) {
            ThemeMode.LIGHT -> ThemeColors.Light
            ThemeMode.DARK -> ThemeColors.Dark
            ThemeMode.SYSTEM -> {
                val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    ThemeColors.Dark
                } else {
                    ThemeColors.Light
                }
            }
        }
    }
}
