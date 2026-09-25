package org.iosclone.keyboard.settings.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IOSGreen = Color(0xFF34C759)
val IOSBlue = Color(0xFF007AFF)
val IOSBlueDark = Color(0xFF0A84FF)

val IOSLightBackground = Color(0xFFF2F2F7)
val IOSLightCardBackground = Color(0xFFFFFFFF)
val IOSLightSeparator = Color(0xFFC6C6C8)
val IOSLightText = Color(0xFF000000)
val IOSLightTextSecondary = Color(0xFF8E8E93)

val IOSDarkBackground = Color(0xFF000000)
val IOSDarkCardBackground = Color(0xFF1C1C1E)
val IOSDarkSeparator = Color(0xFF38383A)
val IOSDarkText = Color(0xFFFFFFFF)
val IOSDarkTextSecondary = Color(0xFF8E8E93)

private val LightColorScheme = lightColorScheme(
    primary = IOSBlue,
    background = IOSLightBackground,
    surface = IOSLightCardBackground,
    onPrimary = Color.White,
    onBackground = IOSLightText,
    onSurface = IOSLightText
)

private val DarkColorScheme = darkColorScheme(
    primary = IOSBlueDark,
    background = IOSDarkBackground,
    surface = IOSDarkCardBackground,
    onPrimary = Color.White,
    onBackground = IOSDarkText,
    onSurface = IOSDarkText
)

@Composable
fun IOSSettingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
