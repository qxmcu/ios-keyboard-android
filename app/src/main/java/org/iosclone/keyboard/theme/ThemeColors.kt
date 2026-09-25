package org.iosclone.keyboard.theme

import android.graphics.Color

/**
 * Pixel-perfect iOS Keyboard color specifications
 */
data class ThemeColors(
    val isDark: Boolean,
    val keyboardBackground: Int,
    val keyBackground: Int,
    val keyBackgroundPressed: Int,
    val keyShadow: Int,
    val modifierKeyBackground: Int,
    val modifierKeyBackgroundPressed: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val accentBlue: Int,
    val returnKeyBlue: Int,
    val returnKeyText: Int,
    val suggestionStripBackground: Int,
    val suggestionStripText: Int,
    val suggestionAutocorrectBackground: Int,
    val popupBackground: Int,
    val popupShadow: Int,
    val gestureTrailColor: Int,
    val trackpadHighlightColor: Int
) {
    companion object {
        // Authentic iOS Light Mode
        val Light = ThemeColors(
            isDark = false,
            keyboardBackground = Color.parseColor("#D1D5DB"), // Light neutral iOS keyboard canvas
            keyBackground = Color.parseColor("#FFFFFF"),      // Pure crisp white key surface
            keyBackgroundPressed = Color.parseColor("#E5E5EA"),// Slight depress tint
            keyShadow = Color.parseColor("#898A8D"),          // Bottom 1dp elevation shadow
            modifierKeyBackground = Color.parseColor("#AFB4BD"),// Shift, Delete, 123 gray keys
            modifierKeyBackgroundPressed = Color.parseColor("#FFFFFF"),
            textPrimary = Color.parseColor("#000000"),
            textSecondary = Color.parseColor("#6C6C70"),
            accentBlue = Color.parseColor("#007AFF"),
            returnKeyBlue = Color.parseColor("#007AFF"),
            returnKeyText = Color.parseColor("#FFFFFF"),
            suggestionStripBackground = Color.parseColor("#ECEFF2"),
            suggestionStripText = Color.parseColor("#000000"),
            suggestionAutocorrectBackground = Color.parseColor("#D9DDE2"),
            popupBackground = Color.parseColor("#FFFFFF"),
            popupShadow = Color.parseColor("#55000000"),
            gestureTrailColor = Color.parseColor("#66007AFF"), // Translucent iOS blue trail
            trackpadHighlightColor = Color.parseColor("#33007AFF")
        )

        // Authentic iOS Dark Mode
        val Dark = ThemeColors(
            isDark = true,
            keyboardBackground = Color.parseColor("#1C1C1E"), // Deep slate iOS dark canvas
            keyBackground = Color.parseColor("#2C2C2E"),      // Elevated key surface
            keyBackgroundPressed = Color.parseColor("#48484A"),
            keyShadow = Color.parseColor("#121213"),          // Dark drop shadow
            modifierKeyBackground = Color.parseColor("#3A3A3C"),// Elevated functional keys
            modifierKeyBackgroundPressed = Color.parseColor("#636366"),
            textPrimary = Color.parseColor("#FFFFFF"),
            textSecondary = Color.parseColor("#8E8E93"),
            accentBlue = Color.parseColor("#0A84FF"),
            returnKeyBlue = Color.parseColor("#0A84FF"),
            returnKeyText = Color.parseColor("#FFFFFF"),
            suggestionStripBackground = Color.parseColor("#252528"),
            suggestionStripText = Color.parseColor("#FFFFFF"),
            suggestionAutocorrectBackground = Color.parseColor("#3A3A3C"),
            popupBackground = Color.parseColor("#2C2C2E"),
            popupShadow = Color.parseColor("#88000000"),
            gestureTrailColor = Color.parseColor("#660A84FF"),
            trackpadHighlightColor = Color.parseColor("#440A84FF")
        )
    }
}
