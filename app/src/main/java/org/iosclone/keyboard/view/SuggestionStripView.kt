package org.iosclone.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
    private val writingToolsBtn: AppleIntelligenceIconView
    private val dividerPaint = Paint().apply {
        strokeWidth = 1f
    }

    private val quickPasteContainer: LinearLayout
    private val quickPasteTv: TextView
    private val quickPasteIcon: ImageView

    var onCandidateSelected: ((String) -> Unit)? = null
    var onQuickPasteSelected: ((String) -> Unit)? = null
    var onWritingToolsRequested: (() -> Unit)? = null

    private var currentQuickPasteText: String? = null
    private var currentTheme: ThemeColors = ThemeColors.Light
    private var isAutocorrectActive = false

    init {
        setWillNotDraw(false)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val density = resources.displayMetrics.density
        val pad = (4 * density).toInt()
        setPadding(pad, 0, pad, 0)

        // Quick paste chip container
        quickPasteContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            val chipPadH = (8 * density).toInt()
            val chipPadV = (4 * density).toInt()
            setPadding(chipPadH, chipPadV, chipPadH, chipPadV)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (6 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E3E5E8"))
                cornerRadius = 14f * density
            }
        }

        quickPasteIcon = ImageView(context).apply {
            layoutParams = LayoutParams((15 * density).toInt(), (15 * density).toInt()).apply {
                marginEnd = (4 * density).toInt()
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
        addView(quickPasteContainer)

        // Apple Intelligence Iridescent Icon Button
        writingToolsBtn = AppleIntelligenceIconView(context).apply {
            val size = (32 * density).toInt()
            layoutParams = LayoutParams(size, size).apply {
                marginEnd = (4 * density).toInt()
            }
            setOnClickListener {
                onWritingToolsRequested?.invoke()
            }
        }
        addView(writingToolsBtn)

        // Candidates row takes 100% available width
        val candidatesLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            gravity = Gravity.CENTER_VERTICAL
        }

        leftCandidateTv = createCandidateView(weight = 1.0f)
        sep1 = createVerticalSeparator().apply { visibility = View.GONE }
        centerCandidateTv = createCandidateView(weight = 1.25f, isCenter = true)
        sep2 = createVerticalSeparator().apply { visibility = View.GONE }
        rightCandidateTv = createCandidateView(weight = 1.0f)

        candidatesLayout.addView(leftCandidateTv)
        candidatesLayout.addView(sep1)
        candidatesLayout.addView(centerCandidateTv)
        candidatesLayout.addView(sep2)
        candidatesLayout.addView(rightCandidateTv)
        addView(candidatesLayout)
    }

    private fun createCandidateView(weight: Float, isCenter: Boolean = false): TextView {
        val density = resources.displayMetrics.density
        return TextView(context).apply {
            layoutParams = LayoutParams(0, (32 * density).toInt(), weight).apply {
                val m = (2 * density).toInt()
                setMargins(m, 0, m, 0)
            }
            gravity = Gravity.CENTER
            textSize = if (isCenter) 16.5f else 14.5f
            typeface = if (isCenter) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
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
        val density = resources.displayMetrics.density
        return View(context).apply {
            layoutParams = LayoutParams(
                (1 * density).toInt().coerceAtLeast(1),
                (22 * density).toInt()
            )
        }
    }

    fun applyTheme(theme: ThemeColors) {
        currentTheme = theme
        setBackgroundColor(Color.TRANSPARENT)

        leftCandidateTv.setTextColor(theme.suggestionStripText)
        centerCandidateTv.setTextColor(theme.suggestionStripText)
        rightCandidateTv.setTextColor(theme.suggestionStripText)

        quickPasteTv.setTextColor(theme.suggestionStripText)
        quickPasteIcon.setColorFilter(theme.suggestionStripText)

        val density = resources.displayMetrics.density
        quickPasteContainer.background = GradientDrawable().apply {
            setColor(if (theme.isDark) 0x33FFFFFF else 0x26000000)
            cornerRadius = 14f * density
            setStroke((1f * density).toInt().coerceAtLeast(1), if (theme.isDark) 0x22FFFFFF else 0x14000000)
        }

        val sepColor = if (theme.isDark) 0x2AFFFFFF else 0x1A000000
        sep1.setBackgroundColor(sepColor)
        sep2.setBackgroundColor(sepColor)
        dividerPaint.color = Color.TRANSPARENT

        writingToolsBtn.setTheme(theme.isDark)

        updateCenterHighlight()
        invalidate()
    }

    private fun updateCenterHighlight() {
        val density = resources.displayMetrics.density
        if (isAutocorrectActive && centerCandidateTv.text.isNotBlank()) {
            centerCandidateTv.background = GradientDrawable().apply {
                setColor(currentTheme.suggestionAutocorrectBackground)
                cornerRadius = 6f * density
            }
        } else {
            centerCandidateTv.background = null
        }
    }

    fun setSuggestions(result: AutocorrectResult) {
        val center = result.centerCandidate
        isAutocorrectActive = result.isAutocorrectCandidate ||
            (!result.isExactMatch && center.isNotEmpty() && !center.equals(result.rawTypedWord, ignoreCase = true))

        val newCenter = if (isAutocorrectActive) "\"$center\"" else center
        if (centerCandidateTv.text != newCenter) {
            centerCandidateTv.text = newCenter
            centerCandidateTv.alpha = 0.6f
            centerCandidateTv.animate().alpha(1.0f).setDuration(100).start()
        }

        if (leftCandidateTv.text != result.leftCandidate) {
            leftCandidateTv.text = result.leftCandidate
            leftCandidateTv.alpha = 0.6f
            leftCandidateTv.animate().alpha(1.0f).setDuration(100).start()
        }

        val rightText = result.suggestedEmoji ?: result.rightCandidate
        if (rightCandidateTv.text != rightText) {
            rightCandidateTv.text = rightText
            rightCandidateTv.alpha = 0.6f
            rightCandidateTv.animate().alpha(1.0f).setDuration(100).start()
        }

        if (result.suggestedEmoji != null) {
            rightCandidateTv.textSize = 20f
            val tf = org.iosclone.keyboard.emoji.EmojiTextView.getBundledTypeface(context)
            if (tf != null && tf != Typeface.DEFAULT) {
                rightCandidateTv.typeface = tf
            }
        } else {
            rightCandidateTv.textSize = 14.5f
            rightCandidateTv.typeface = Typeface.DEFAULT
        }

        val hasLeft = result.leftCandidate.isNotBlank()
        val hasCenter = center.isNotBlank()
        val hasRight = rightText.isNotBlank()

        sep1.visibility = if (hasLeft && hasCenter) View.VISIBLE else View.GONE
        sep2.visibility = if (hasCenter && hasRight) View.VISIBLE else View.GONE

        updateCenterHighlight()
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
        isAutocorrectActive = false
        updateCenterHighlight()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }
}
