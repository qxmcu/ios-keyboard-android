package org.iosclone.keyboard.emoji

import android.content.ClipDescription
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object EmojiStickerHelper {

    private const val TAG = "EmojiStickerHelper"

    /**
     * Checks if the active target input field supports rich image/sticker content.
     */
    fun supportsRichMedia(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        return mimeTypes.any { it.equals("image/png", ignoreCase = true) || it.equals("image/*", ignoreCase = true) }
    }

    /**
     * Attempts to commit the genuine Apple iOS emoji as a high-definition 256x256 transparent PNG sticker
     * into apps that support rich content (such as Instagram Stories, Telegram, WhatsApp, Discord).
     * Returns true if successfully committed, or false to fallback to standard Unicode text.
     */
    fun commitEmojiSticker(
        context: Context,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        emoji: String
    ): Boolean {
        if (inputConnection == null || editorInfo == null) return false
        if (!supportsRichMedia(editorInfo)) return false

        return try {
            val stickerFile = renderEmojiToPng(context, emoji) ?: return false
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, stickerFile)

            val description = ClipDescription("iOS Emoji Sticker", arrayOf("image/png"))
            val inputContentInfo = InputContentInfoCompat(contentUri, description, null)

            val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            InputConnectionCompat.commitContent(inputConnection, editorInfo, inputContentInfo, flags, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to commit emoji sticker: ${e.message}", e)
            false
        }
    }

    /**
     * Renders the authentic AppleColorEmoji font glyph into a 256x256 transparent PNG.
     */
    fun renderEmojiToPng(context: Context, emoji: String): File? {
        return try {
            val dir = File(context.cacheDir, "emoji_stickers")
            if (!dir.exists()) dir.mkdirs()

            val fileName = "emoji_${emoji.hashCode().toUInt().toString(16)}.png"
            val file = File(dir, fileName)
            if (file.exists() && file.length() > 0) return file

            val size = 256
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = EmojiTextView.getBundledTypeface(context)
                textSize = 180f
                textAlign = Paint.Align.CENTER
            }

            val textBounds = Rect()
            paint.getTextBounds(emoji, 0, emoji.length, textBounds)
            val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)

            canvas.drawText(emoji, size / 2f, yPos, paint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render emoji PNG: ${e.message}", e)
            null
        }
    }

    /**
     * Exports the bundled AppleColorEmoji.ttf directly to the public Downloads folder
     * for users to easily apply system-wide on Nothing Phone or custom OEM Android via zFont 3 / Shizuku.
     */
    fun exportEmojiFontToDownloads(context: Context): String? {
        val fileName = "iOS_26.4_AppleColorEmoji.ttf"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "font/ttf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { out ->
                    context.assets.open("fonts/AppleColorEmoji.ttf").use { input ->
                        input.copyTo(out)
                    }
                }
                "Downloads/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    context.assets.open("fonts/AppleColorEmoji.ttf").use { input ->
                        input.copyTo(out)
                    }
                }
                targetFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export emoji font: ${e.message}", e)
            null
        }
    }
}
