package org.iosclone.keyboard.theme

import android.graphics.Color

/**
 * Pixel-perfect iOS 27 Clear Liquid Glass & Dark Tinted Glass color specifications.
 * Matches authentic translucent blurred backdrop and luminous pebble keys.
 */
data class ThemeColors(
    val isDark: Boolean,
    val keyboardBackground: Int,
    val keyboardGlassStroke: Int,
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
        // Authentic iOS 27 Clear Liquid Glass UI (Light Mode)
        val Light = ThemeColors(
            isDark = false,
            keyboardBackground = Color.parseColor("#EBF0F3F7"), // Frosted luminous liquid glass canvas (less transparent, lighter)
            keyboardGlassStroke = Color.TRANSPARENT,            // Zero white outlines
            keyBackground = Color.parseColor("#F5FFFFFF"),      // Luminous rounded white glass pebble
            keyBackgroundPressed = Color.parseColor("#D4CFD4DC"),
            keyShadow = Color.parseColor("#26000000"),          // Soft ambient drop shadow
            modifierKeyBackground = Color.parseColor("#D4D8DFE7"),// Subtle tinted modifier glass
            modifierKeyBackgroundPressed = Color.parseColor("#F5FFFFFF"),
            textPrimary = Color.parseColor("#1C1C1E"),          // Crisp dark charcoal iOS typography
            textSecondary = Color.parseColor("#8E8E93"),
            accentBlue = Color.parseColor("#007AFF"),
            returnKeyBlue = Color.parseColor("#007AFF"),
            returnKeyText = Color.parseColor("#FFFFFF"),
            suggestionStripBackground = Color.parseColor("#00000000"), // 100% transparent glass
            suggestionStripText = Color.parseColor("#1C1C1E"),
            suggestionAutocorrectBackground = Color.parseColor("#33000000"),
            popupBackground = Color.parseColor("#F5FFFFFF"),
            popupShadow = Color.parseColor("#40000000"),
            gestureTrailColor = Color.parseColor("#66007AFF"),
            trackpadHighlightColor = Color.parseColor("#33007AFF")
        )

        // Authentic iOS 27 Tinted Liquid Glass UI (Dark Mode)
        val Dark = ThemeColors(
            isDark = true,
            keyboardBackground = Color.parseColor("#EE121215"), // Rich deep charcoal liquid glass canvas (less transparent, darker)
            keyboardGlassStroke = Color.TRANSPARENT,            // Zero white outlines
            keyBackground = Color.parseColor("#944E4E54"),      // Translucent slate glass pebble
            keyBackgroundPressed = Color.parseColor("#CC5E5E64"),
            keyShadow = Color.parseColor("#4D000000"),          // Dark depth shadow
            modifierKeyBackground = Color.parseColor("#802F2F34"),// Deep translucent functional glass
            modifierKeyBackgroundPressed = Color.parseColor("#944E4E54"),
            textPrimary = Color.parseColor("#FFFFFF"),          // Pure luminous white font
            textSecondary = Color.parseColor("#8E8E93"),
            accentBlue = Color.parseColor("#0A84FF"),
            returnKeyBlue = Color.parseColor("#0A84FF"),
            returnKeyText = Color.parseColor("#FFFFFF"),
            suggestionStripBackground = Color.parseColor("#00000000"), // 100% transparent glass
            suggestionStripText = Color.parseColor("#FFFFFF"),
            suggestionAutocorrectBackground = Color.parseColor("#33FFFFFF"),
            popupBackground = Color.parseColor("#E0242428"),
            popupShadow = Color.parseColor("#80000000"),
            gestureTrailColor = Color.parseColor("#660A84FF"),
            trackpadHighlightColor = Color.parseColor("#440A84FF")
        )
    }
}
