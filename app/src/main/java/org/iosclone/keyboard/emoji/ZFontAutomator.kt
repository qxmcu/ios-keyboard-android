package org.iosclone.keyboard.emoji

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Automated zFont 3 integration engine for non-Nothing Android devices (Samsung, Xiaomi, Oppo, OnePlus, Vivo, etc.).
 * Automatically exports AppleColorEmoji.ttf, queries zFont 3 status, and dispatches direct ACTION_VIEW intents
 * to launch the OEM-specific 1-tap font installer without tedious manual file searching or configuration.
 */
object ZFontAutomator {

    private const val TAG = "ZFontAutomator"
    const val ZFONT_PACKAGE = "com.mgng.zfont3"
    private const val PLAY_STORE_MARKET = "market://details?id=$ZFONT_PACKAGE"
    private const val PLAY_STORE_WEB = "https://play.google.com/store/apps/details?id=$ZFONT_PACKAGE"
    const val FONT_FILE_NAME = "iOS_26.4_AppleColorEmoji.ttf"

    /**
     * Checks if zFont 3 is installed on this device.
     */
    fun isZFontInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ZFONT_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking zFont package: ${e.message}")
            false
        }
    }

    /**
     * Opens the Google Play Store page for zFont 3.
     */
    fun openPlayStore(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_MARKET)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.vending")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open Play Store: ${e.message}")
            }
        }
    }

    /**
     * Extracts AppleColorEmoji.ttf into app external files directory for zFont 3 access.
     * Caches the file to ensure sub-millisecond execution and zero battery drain.
     */
    fun prepareFontFile(context: Context): File? {
        return try {
            val dir = context.getExternalFilesDir("fonts") ?: File(context.filesDir, "fonts")
            if (!dir.exists()) dir.mkdirs()

            val targetFile = File(dir, FONT_FILE_NAME)
            if (targetFile.exists() && targetFile.length() > 1_000_000) {
                return targetFile
            }

            context.assets.open("fonts/AppleColorEmoji.ttf").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare font file: ${e.message}", e)
            null
        }
    }

    /**
     * Dispatches AppleColorEmoji.ttf directly to zFont 3 via ACTION_VIEW intent.
     * Grants URI read permission so zFont 3 directly loads the font into its apply screen.
     */
    fun launchZFontWithFont(context: Context): Boolean {
        val fontFile = prepareFontFile(context) ?: return false
        val authority = "${context.packageName}.fileprovider"

        return try {
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, fontFile)

            // Explicitly grant permission to zFont 3 package
            try {
                context.grantUriPermission(ZFONT_PACKAGE, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {}

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "font/ttf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(ZFONT_PACKAGE)
            }

            context.startActivity(viewIntent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct ACTION_VIEW failed, falling back to launch intent: ${e.message}")
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(ZFONT_PACKAGE)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    true
                } else {
                    false
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback launch failed: ${e2.message}")
                false
            }
        }
    }

    /**
     * Automatic entry point:
     * - If zFont 3 is installed: extracts font, launches zFont 3 directly on font apply screen.
     * - If zFont 3 is not installed: shows automated setup dialog prompting 1-tap install.
     */
    fun autoApplyIOSFont(context: Context, windowToken: IBinder? = null): Boolean {
        // Also pre-export to public Downloads so zFont 3 can find it anywhere
        EmojiStickerHelper.exportEmojiFontToDownloads(context)

        if (!isZFontInstalled(context)) {
            showSetupDialog(context, windowToken)
            return false
        }

        val success = launchZFontWithFont(context)
        if (success) {
            val brand = DeviceDetector.getShortBrandName()
            Toast.makeText(
                context,
                "🚀 Loaded iOS 26.4 Emojis into zFont 3! Tap 'Apply' to enable on $brand.",
                Toast.LENGTH_LONG
            ).show()
        }
        return success
    }

    /**
     * Shows 1-tap setup dialog explaining rootless iOS emoji apply on user's device.
     */
    fun showSetupDialog(context: Context, windowToken: IBinder? = null) {
        val brand = DeviceDetector.getBrandDisplayName()
        val builder = AlertDialog.Builder(context)
            .setTitle("Auto-Apply iOS 26.4 Emojis ($brand)")
            .setMessage(
                "Genuine Apple iOS emojis can be applied across all apps on $brand without root using zFont 3.\n\n" +
                "1. Tap 'Install zFont 3' from Google Play (free, no root required).\n\n" +
                "2. Once installed, tap 'Auto-Apply' to load AppleColorEmoji.ttf directly into the $brand system font installer!"
            )
            .setPositiveButton("Install zFont 3") { _, _ ->
                prepareFontFile(context)
                EmojiStickerHelper.exportEmojiFontToDownloads(context)
                openPlayStore(context)
            }
            .setNeutralButton("How It Works") { _, _ ->
                showOEMInstructionsDialog(context, windowToken)
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        attachWindowTokenIfNeeded(context, dialog, windowToken)
        try {
            dialog.show()
        } catch (_: Exception) {
            openPlayStore(context)
        }
    }

    /**
     * Shows OEM-specific instructions dialog.
     */
    fun showOEMInstructionsDialog(context: Context, windowToken: IBinder? = null) {
        val brand = DeviceDetector.getBrandDisplayName()
        val guide = DeviceDetector.getOEMGuideDetails()

        val builder = AlertDialog.Builder(context)
            .setTitle("$brand iOS Emoji Guide")
            .setMessage(guide)
            .setPositiveButton("Got It", null)
            .setNegativeButton("Install zFont 3") { _, _ ->
                openPlayStore(context)
            }

        val dialog = builder.create()
        attachWindowTokenIfNeeded(context, dialog, windowToken)
        try {
            dialog.show()
        } catch (_: Exception) {}
    }

    private fun attachWindowTokenIfNeeded(context: Context, dialog: AlertDialog, windowToken: IBinder?) {
        if (context !is Activity && windowToken != null) {
            try {
                dialog.window?.attributes?.token = windowToken
                dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
            } catch (_: Throwable) {}
        }
    }
}
