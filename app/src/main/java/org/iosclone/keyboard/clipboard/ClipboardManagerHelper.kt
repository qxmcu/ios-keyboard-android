package org.iosclone.keyboard.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.util.Log

class ClipboardManagerHelper(private val context: Context) {

    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val db = ClipboardDatabase(context)

    var onNewClipAvailable: ((String) -> Unit)? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkCurrentClip()
    }

    fun startListening() {
        clipboard?.addPrimaryClipChangedListener(clipListener)
        checkCurrentClip()
    }

    fun stopListening() {
        clipboard?.removePrimaryClipChangedListener(clipListener)
    }

    fun getRecentClips(): List<ClipboardEntry> {
        return db.getAllClips()
    }

    fun pinClip(id: Long, isPinned: Boolean) {
        db.setPinned(id, isPinned)
    }

    fun deleteClip(id: Long) {
        db.deleteClip(id)
    }

    fun clearHistory() {
        db.clearAll()
    }

    fun copyToClipboard(text: String) {
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun checkCurrentClip() {
        try {
            val clipData = clipboard?.primaryClip ?: return
            if (clipData.itemCount > 0) {
                val description = clipboard.primaryClipDescription
                // Don't capture password/sensitive fields
                if (description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                    description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true
                ) {
                    val text = clipData.getItemAt(0)?.text?.toString()?.trim() ?: return
                    if (text.isNotEmpty() && text.length <= 4000) {
                        db.addClip(text)
                        onNewClipAvailable?.invoke(text)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ClipboardManagerHelper", "Error checking primary clip", e)
        }
    }
}
