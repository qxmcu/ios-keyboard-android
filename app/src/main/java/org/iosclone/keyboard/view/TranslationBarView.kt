package org.iosclone.keyboard.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.iosclone.keyboard.theme.ThemeColors

class TranslationBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onSwapLanguages: (() -> Unit)? = null
    var onInsertClicked: ((String) -> Unit)? = null
    var onCloseClicked: (() -> Unit)? = null

    private val langPairTv: TextView
    private val previewTv: TextView
    private val insertBtn: TextView
    private val closeBtn: ImageView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val density = resources.displayMetrics.density
        val padH = (12 * density).toInt()
        setPadding(padH, 0, padH, 0)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (38 * density).toInt())

        langPairTv = TextView(context).apply {
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val pad = (4 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener { onSwapLanguages?.invoke() }
        }

        previewTv = TextView(context).apply {
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginStart = (8 * density).toInt()
                marginEnd = (8 * density).toInt()
            }
        }

        insertBtn = TextView(context).apply {
            text = "Insert"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val pad = (6 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener { onInsertClicked?.invoke(previewTv.text.toString()) }
        }

        closeBtn = ImageView(context).apply {
            val size = (24 * density).toInt()
            layoutParams = LayoutParams(size, size)
            val pad = (4 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setOnClickListener { onCloseClicked?.invoke() }
        }

        addView(langPairTv)
        addView(previewTv)
        addView(insertBtn)
        addView(closeBtn)
    }

    fun setLanguages(source: String, target: String) {
        langPairTv.text = "${source.uppercase()} ⇄ ${target.uppercase()}"
    }

    fun setTranslationPreview(translatedText: String) {
        previewTv.text = translatedText
    }

    fun applyTheme(theme: ThemeColors) {
        setBackgroundColor(theme.suggestionStripBackground)
        langPairTv.setTextColor(theme.accentBlue)
        previewTv.setTextColor(theme.textPrimary)
        insertBtn.setTextColor(theme.accentBlue)
        closeBtn.setColorFilter(theme.textSecondary)
    }
}
