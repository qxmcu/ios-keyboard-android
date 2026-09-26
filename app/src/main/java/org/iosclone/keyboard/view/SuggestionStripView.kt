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
    private val dividerPaint = Paint().apply {
        strokeWidth = 1f
    }

    private val quickPasteContainer: LinearLayout
    private val quickPasteTv: TextView
    private val quickPasteIcon: ImageView

    var onCandidateSelected: ((String) -> Unit)? = null
    var onQuickPasteSelected: ((String) -> Unit)? = null

    private var currentQuickPasteText: String? = null
    private var currentTheme: ThemeColors = ThemeColors.Light
    private var isAutocorrectActive = false

    init {
        setWillNotDraw(false)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = (4 * resources.displayMetrics.density).toInt()
        setPadding(pad, 0, pad, 0)

        // Quick paste chip container
        val density = resources.displayMetrics.density
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
        setBackgroundColor(theme.suggestionStripBackground)

        leftCandidateTv.setTextColor(theme.suggestionStripText)
        centerCandidateTv.setTextColor(theme.suggestionStripText)
        rightCandidateTv.setTextColor(theme.suggestionStripText)

        quickPasteTv.setTextColor(theme.suggestionStripText)
        quickPasteIcon.setColorFilter(theme.accentBlue)

        val sepColor = if (theme.isDark) 0x2AFFFFFF else 0x1A000000
        sep1.setBackgroundColor(sepColor)
        sep2.setBackgroundColor(sepColor)
        dividerPaint.color = if (theme.isDark) 0x2EFFFFFF.toInt() else 0x1F000000

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
        isAutocorrectActive = !result.isExactMatch && center.isNotEmpty() && !center.equals(result.rawTypedWord, ignoreCase = true)

        centerCandidateTv.text = if (isAutocorrectActive) "\"$center\"" else center
        leftCandidateTv.text = result.leftCandidate
        rightCandidateTv.text = result.rightCandidate

        val hasLeft = result.leftCandidate.isNotBlank()
        val hasCenter = center.isNotBlank()
        val hasRight = result.rightCandidate.isNotBlank()

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
        val y = height.toFloat() - 1f
        canvas.drawLine(0f, y, width.toFloat(), y, dividerPaint)
    }
}
