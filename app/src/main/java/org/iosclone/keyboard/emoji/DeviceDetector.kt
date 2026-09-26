package org.iosclone.keyboard.emoji

import android.os.Build

/**
 * Intelligent hardware & OEM ROM detector for iOS Keyboard.
 * Automatically identifies Nothing OS vs OEM Android distributions (Samsung One UI,
 * Xiaomi HyperOS/MIUI, Oppo/Realme ColorOS, Vivo Funtouch OS, Google Pixel, etc.)
 * to tailor emoji integration and font injection mechanisms.
 */
object DeviceDetector {

    /**
     * Checks if the active device is a Nothing Phone (Phone 1, Phone 2, Phone 2a, Phone 2a Plus, CMF Phone 1).
     */
    fun isNothingPhone(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        return m.contains("nothing") ||
                b.contains("nothing") ||
                model.contains("nothing") ||
                model.contains("a063") || // Nothing Phone 1
                model.contains("a065") || // Nothing Phone 2
                model.contains("ain065") || // Nothing Phone 2a
                model.contains("a142") || // Nothing Phone 2a Plus
                model.contains("a001")    // CMF Phone 1
    }

    /**
     * Returns full user-friendly brand and OS name for settings and setup dialogs.
     */
    fun getBrandDisplayName(): String {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return when {
            isNothingPhone() -> "Nothing Phone (Nothing OS)"
            m.contains("samsung") || b.contains("samsung") -> "Samsung Galaxy (One UI)"
            m.contains("xiaomi") || b.contains("xiaomi") || m.contains("redmi") || b.contains("redmi") || m.contains("poco") -> "Xiaomi / Poco (HyperOS & MIUI)"
            m.contains("oneplus") || b.contains("oneplus") -> "OnePlus (OxygenOS)"
            m.contains("oppo") || b.contains("oppo") -> "Oppo (ColorOS)"
            m.contains("realme") || b.contains("realme") -> "Realme (Realme UI)"
            m.contains("vivo") || b.contains("vivo") || m.contains("iqoo") -> "Vivo / iQOO (Funtouch OS)"
            m.contains("google") || b.contains("google") -> "Google Pixel"
            m.contains("motorola") || b.contains("motorola") || m.contains("moto") -> "Motorola"
            m.contains("huawei") || b.contains("huawei") || m.contains("honor") || b.contains("honor") -> "Huawei / Honor"
            else -> Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Returns compact brand name for keyboard chips and action buttons.
     */
    fun getShortBrandName(): String {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return when {
            isNothingPhone() -> "Nothing Phone"
            m.contains("samsung") || b.contains("samsung") -> "Samsung"
            m.contains("xiaomi") || b.contains("xiaomi") || m.contains("redmi") || b.contains("redmi") || m.contains("poco") -> "Xiaomi"
            m.contains("oneplus") || b.contains("oneplus") -> "OnePlus"
            m.contains("oppo") || b.contains("oppo") -> "Oppo"
            m.contains("realme") || b.contains("realme") -> "Realme"
            m.contains("vivo") || b.contains("vivo") || m.contains("iqoo") -> "Vivo"
            m.contains("google") || b.contains("google") -> "Pixel"
            m.contains("motorola") || b.contains("motorola") || m.contains("moto") -> "Motorola"
            else -> Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Returns OEM-specific instructions for automated font apply in zFont 3.
     */
    fun getOEMGuideDetails(): String {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return when {
            m.contains("samsung") || b.contains("samsung") ->
                "On Samsung Galaxy (One UI 1 to 6/7):\n\n" +
                "1. zFont 3 automatically prepares the Samsung FlipFont pack.\n" +
                "2. Tap 'Apply' in zFont 3 -> follow the auto-steps (Install Samsung Sans, Backup Settings, Install AppleColorEmoji, Restore Settings).\n" +
                "3. In Settings > Display > Font size and style > Font style, select 'AppleColorEmoji'.\n\n" +
                "Authentic iOS emojis will now display across Samsung Notes, Messages, Instagram, and all apps!"

            m.contains("xiaomi") || b.contains("xiaomi") || m.contains("redmi") || b.contains("redmi") || m.contains("poco") ->
                "On Xiaomi / Redmi / Poco (HyperOS & MIUI):\n\n" +
                "1. zFont 3 creates the official MIUI/HyperOS Theme font.\n" +
                "2. Tap 'Apply' -> select 'Method 2 (Theme Directory)'.\n" +
                "3. Open Themes app > Profile > Fonts > select iOS_26.4_AppleColorEmoji > Apply and Reboot.\n\n" +
                "Authentic iOS emojis will display system-wide across all apps!"

            m.contains("oppo") || b.contains("oppo") || m.contains("realme") || b.contains("realme") || m.contains("oneplus") || b.contains("oneplus") ->
                "On Oppo / Realme / OnePlus (ColorOS & OxygenOS):\n\n" +
                "1. Tap 'Apply' in zFont 3 -> choose 'Support DAI Characters'.\n" +
                "2. In Settings > Language & Region, set Region to 'Myanmar'.\n" +
                "3. Under Settings > Display & Brightness, enable 'Support DAI Characters'.\n\n" +
                "All iOS emojis will immediately render across all apps!"

            m.contains("vivo") || b.contains("vivo") || m.contains("iqoo") ->
                "On Vivo / iQOO (Funtouch OS):\n\n" +
                "1. Tap 'Apply' in zFont 3 -> choose 'Funtouch OS method'.\n" +
                "2. Open iTheme app > Local Fonts -> select iOS_26.4_AppleColorEmoji and apply.\n\n" +
                "All apps will now display Apple iOS emojis!"

            else ->
                "On ${getBrandDisplayName()}:\n\n" +
                "1. zFont 3 automatically loads AppleColorEmoji.ttf.\n" +
                "2. Tap 'Apply' in zFont 3 and select the recommended method for your device (Theme Store, Shizuku rootless, or Magisk).\n" +
                "3. Enjoy genuine Apple iOS emojis in all apps!"
        }
    }
}
