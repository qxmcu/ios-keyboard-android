package org.iosclone.keyboard.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.iosclone.keyboard.clipboard.ClipboardEntry
import org.iosclone.keyboard.clipboard.ClipboardManagerHelper
import org.iosclone.keyboard.theme.ThemeColors
import androidx.core.content.ContextCompat

class ClipboardDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onClipSelected: ((String) -> Unit)? = null
    var onCloseRequested: (() -> Unit)? = null

    private val headerTitle: TextView
    private val closeBtn: TextView
    private val clearAllBtn: TextView
    private val clipsContainer: LinearLayout
    private val scrollView: ScrollView

    private var clipboardHelper: ClipboardManagerHelper? = null

    init {
        orientation = VERTICAL
        val density = resources.displayMetrics.density

        // 1. Top Action Header
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (14 * density).toInt()
            val padV = (8 * density).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (44 * density).toInt())
        }

        headerTitle = TextView(context).apply {
            text = "Clipboard"
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }

        clearAllBtn = TextView(context).apply {
            text = "Clear All"
            textSize = 14f
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                clipboardHelper?.clearHistory()
                refreshClips()
            }
        }

        closeBtn = TextView(context).apply {
            text = "Done"
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener { onCloseRequested?.invoke() }
        }

        header.addView(headerTitle)
        header.addView(clearAllBtn)
        header.addView(closeBtn)
        addView(header)

        // 2. Scrollable list of clips
        scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
        }

        clipsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            val pad = (8 * density).toInt()
            setPadding(pad, 0, pad, pad)
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        scrollView.addView(clipsContainer)
        addView(scrollView)
    }

    fun bindHelper(helper: ClipboardManagerHelper) {
        this.clipboardHelper = helper
        refreshClips()
    }

    fun refreshClips() {
        val helper = clipboardHelper ?: return
        val clips = helper.getRecentClips()
        clipsContainer.removeAllViews()

        val density = resources.displayMetrics.density

        if (clips.isEmpty()) {
            val emptyTv = TextView(context).apply {
                text = "Clipboard is empty"
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, (40 * density).toInt(), 0, 0)
            }
            clipsContainer.addView(emptyTv)
            return
        }

        for (clip in clips) {
            val card = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val cardPad = (10 * density).toInt()
                setPadding(cardPad, cardPad, cardPad, cardPad)
                val marginV = (4 * density).toInt()
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, marginV, 0, marginV)
                }
                background = ContextCompat.getDrawable(context, android.R.drawable.dialog_holo_light_frame)
            }

            val textTv = TextView(context).apply {
                text = clip.text
                textSize = 14f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
            }

            // Pin button
            val pinBtn = TextView(context).apply {
                text = if (clip.isPinned) "📌" else "📍"
                textSize = 16f
                val pad = (6 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    helper.pinClip(clip.id, !clip.isPinned)
                    refreshClips()
                }
            }

            // Delete button
            val deleteBtn = TextView(context).apply {
                text = "✕"
                textSize = 15f
                val pad = (6 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    helper.deleteClip(clip.id)
                    refreshClips()
                }
            }

            card.setOnClickListener {
                onClipSelected?.invoke(clip.text)
            }

            card.addView(textTv)
            card.addView(pinBtn)
            card.addView(deleteBtn)
            clipsContainer.addView(card)
        }
    }

    fun applyTheme(theme: ThemeColors) {
        setBackgroundColor(theme.keyboardBackground)
        headerTitle.setTextColor(theme.textPrimary)
        closeBtn.setTextColor(theme.accentBlue)
        clearAllBtn.setTextColor(theme.accentBlue)
    }
}
