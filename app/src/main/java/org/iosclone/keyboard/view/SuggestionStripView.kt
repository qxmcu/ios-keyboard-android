package org.iosclone.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.iosclone.keyboard.dictionary.AutocorrectResult
import org.iosclone.keyboard.theme.ThemeColors

class SuggestionStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val leftCandidateTv: TextView
    private val centerCandidateTv: TextView
    private val rightCandidateTv: TextView
    private val sep1: View
    private val sep2: View
    private val dividerPaint = Paint().apply {
        strokeWidth = 1f
    }

    private val quickPasteContainer: LinearLayout
    private val quickPasteTv: TextView
    private val quickPasteIcon: ImageView

    private val actionContainer: LinearLayout
    private val translateBtn: ImageView
    private val clipboardBtn: ImageView
    private val undoBtn: ImageView

    var onCandidateSelected: ((String) -> Unit)? = null
    var onQuickPasteSelected: ((String) -> Unit)? = null
    var onTranslateClicked: (() -> Unit)? = null
    var onClipboardClicked: (() -> Unit)? = null
    var onUndoClicked: (() -> Unit)? = null

    private var currentQuickPasteText: String? = null

    init {
        setWillNotDraw(false)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = (6 * resources.displayMetrics.density).toInt()
        setPadding(pad, 0, pad, 0)

        // Candidates row
        val candidatesLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            gravity = Gravity.CENTER_VERTICAL
        }

        leftCandidateTv = createCandidateView(weight = 1.0f)
        sep1 = createVerticalSeparator().apply { visibility = View.GONE }
        centerCandidateTv = createCandidateView(weight = 1.2f, isCenter = true)
        sep2 = createVerticalSeparator().apply { visibility = View.GONE }
        rightCandidateTv = createCandidateView(weight = 1.0f)

        candidatesLayout.addView(leftCandidateTv)
        candidatesLayout.addView(sep1)
        candidatesLayout.addView(centerCandidateTv)
        candidatesLayout.addView(sep2)
        candidatesLayout.addView(rightCandidateTv)

        // Quick paste chip container
        quickPasteContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            val chipPadH = (8 * resources.displayMetrics.density).toInt()
            val chipPadV = (4 * resources.displayMetrics.density).toInt()
            setPadding(chipPadH, chipPadV, chipPadH, chipPadV)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
        }

        quickPasteIcon = ImageView(context).apply {
            layoutParams = LayoutParams((16 * resources.displayMetrics.density).toInt(), (16 * resources.displayMetrics.density).toInt()).apply {
                marginEnd = (4 * resources.displayMetrics.density).toInt()
            }
            val resId = context.resources.getIdentifier("ic_clipboard", "drawable", context.packageName)
            if (resId != 0) setImageResource(resId)
        }

        quickPasteTv = TextView(context).apply {
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        quickPasteContainer.addView(quickPasteIcon)
        quickPasteContainer.addView(quickPasteTv)
        quickPasteContainer.setOnClickListener {
            currentQuickPasteText?.let { onQuickPasteSelected?.invoke(it) }
        }

        // Action shortcuts
        actionContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        }

        val iconSize = (18 * resources.displayMetrics.density).toInt()
        val iconPad = (6 * resources.displayMetrics.density).toInt()

        undoBtn = createActionButton("ic_undo", iconSize, iconPad) { onUndoClicked?.invoke() }
        translateBtn = createActionButton("ic_translate", iconSize, iconPad) { onTranslateClicked?.invoke() }
        clipboardBtn = createActionButton("ic_clipboard", iconSize, iconPad) { onClipboardClicked?.invoke() }

        actionContainer.addView(undoBtn)
        actionContainer.addView(translateBtn)
        actionContainer.addView(clipboardBtn)

        addView(quickPasteContainer)
        addView(candidatesLayout)
        addView(actionContainer)
    }

    private fun createCandidateView(weight: Float, isCenter: Boolean = false): TextView {
        return TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
            gravity = Gravity.CENTER
            textSize = if (isCenter) 16f else 14f
            if (isCenter) typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener {
                val text = this.text.toString().replace("\"", "").trim()
                if (text.isNotEmpty()) {
                    onCandidateSelected?.invoke(text)
                }
            }
        }
    }

    private fun createVerticalSeparator(): View {
        return View(context).apply {
            layoutParams = LayoutParams(
                (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1),
                (20 * resources.displayMetrics.density).toInt()
            )
        }
    }

    private fun createActionButton(resName: String, size: Int, padding: Int, onClick: () -> Unit): ImageView {
        return ImageView(context).apply {
            layoutParams = LayoutParams(size + (padding * 2), size + (padding * 2))
            setPadding(padding, padding, padding, padding)
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (resId != 0) setImageResource(resId)
            setOnClickListener { onClick() }
        }
    }

    fun applyTheme(theme: ThemeColors) {
        setBackgroundColor(theme.suggestionStripBackground)

        leftCandidateTv.setTextColor(theme.suggestionStripText)
        centerCandidateTv.setTextColor(theme.suggestionStripText)
        rightCandidateTv.setTextColor(theme.suggestionStripText)

        quickPasteTv.setTextColor(theme.suggestionStripText)
        quickPasteIcon.setColorFilter(theme.accentBlue)

        translateBtn.setColorFilter(theme.textSecondary)
        clipboardBtn.setColorFilter(theme.textSecondary)
        undoBtn.setColorFilter(theme.textSecondary)

        val sepColor = if (theme.isDark) 0x33FFFFFF else 0x22000000
        sep1.setBackgroundColor(sepColor)
        sep2.setBackgroundColor(sepColor)
        dividerPaint.color = if (theme.isDark) 0x2EFFFFFF.toInt() else 0x1F000000
        invalidate()
    }

    fun setSuggestions(result: AutocorrectResult) {
        val center = result.centerCandidate
        val isAutocorrect = !result.isExactMatch && center.isNotEmpty() && !center.equals(result.rawTypedWord, ignoreCase = true)

        centerCandidateTv.text = if (isAutocorrect) "\"$center\"" else center
        leftCandidateTv.text = result.leftCandidate
        rightCandidateTv.text = result.rightCandidate

        val hasLeft = result.leftCandidate.isNotBlank()
        val hasCenter = center.isNotBlank()
        val hasRight = result.rightCandidate.isNotBlank()

        sep1.visibility = if (hasLeft && hasCenter) View.VISIBLE else View.GONE
        sep2.visibility = if (hasCenter && hasRight) View.VISIBLE else View.GONE
    }

    fun showQuickPaste(text: String?) {
        currentQuickPasteText = text
        if (text != null && text.isNotBlank()) {
            quickPasteContainer.visibility = View.VISIBLE
            val preview = if (text.length > 14) text.take(12) + "…" else text
            quickPasteTv.text = preview
        } else {
            quickPasteContainer.visibility = View.GONE
        }
    }

    fun clearSuggestions() {
        centerCandidateTv.text = ""
        leftCandidateTv.text = ""
        rightCandidateTv.text = ""
        sep1.visibility = View.GONE
        sep2.visibility = View.GONE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val y = height.toFloat() - 1f
        canvas.drawLine(0f, y, width.toFloat(), y, dividerPaint)
    }
}
