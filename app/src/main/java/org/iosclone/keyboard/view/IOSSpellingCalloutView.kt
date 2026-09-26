package org.iosclone.keyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import org.iosclone.keyboard.theme.ThemeColors

/**
 * Authentic iOS Callout Bubble for misspelled or autocorrected words.
 * Floats independently in a PopupWindow above the keyboard, completely separate
 * from the keyboard layout hierarchy so it never shifts keys or affects sizing.
 */
class IOSSpellingCalloutView(private val context: Context) {

    private val density = context.resources.displayMetrics.density
    private var popupWindow: PopupWindow? = null
    private val containerLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = (8 * density).toInt()
        val padV = (5 * density).toInt()
        setPadding(padH, padV, padH, padV)
    }

    private var onCandidateClickListener: ((String) -> Unit)? = null
    private var onNeverAutocorrectListener: ((String) -> Unit)? = null

    fun showCallout(
        anchorView: View,
        word: String,
        candidates: List<String>,
        revertOption: String? = null,
        theme: ThemeColors,
        onCandidateSelected: (String) -> Unit,
        onNeverAutocorrect: (String) -> Unit
    ) {
        if (!anchorView.isAttachedToWindow) return

        this.onCandidateClickListener = onCandidateSelected
        this.onNeverAutocorrectListener = onNeverAutocorrect

        dismiss()

        containerLayout.removeAllViews()

        // Background styling: Authentic iOS Floating Glass Bubble
        containerLayout.background = GradientDrawable().apply {
            setColor(if (theme.isDark) Color.parseColor("#E6252528") else Color.parseColor("#F5F5F7"))
            cornerRadius = 18f * density
            setStroke(
                (1f * density).toInt().coerceAtLeast(1),
                if (theme.isDark) Color.parseColor("#44FFFFFF") else Color.parseColor("#25000000")
            )
        }

        // 1. Revert button if word was autocorrected
        if (revertOption != null && revertOption.isNotBlank() && !revertOption.equals(word, ignoreCase = true)) {
            val revertTv = createItemView("↩ \"$revertOption\"", isBold = true, isAccent = true, theme = theme, density = density) {
                onCandidateClickListener?.invoke(revertOption)
                dismiss()
            }
            containerLayout.addView(revertTv)
            containerLayout.addView(createDivider(theme, density))
        }

        // 2. Candidate suggestions
        val displayCandidates = candidates.filter { !it.equals(revertOption, ignoreCase = true) }.take(3)
        for ((idx, cand) in displayCandidates.withIndex()) {
            val candTv = createItemView(cand, isBold = false, isAccent = false, theme = theme, density = density) {
                onCandidateClickListener?.invoke(cand)
                dismiss()
            }
            containerLayout.addView(candTv)
            if (idx < displayCandidates.lastIndex) {
                containerLayout.addView(createDivider(theme, density))
            }
        }

        // 3. "Learn word" action chip if word is unrecognized
        if (word.isNotBlank()) {
            containerLayout.addView(createDivider(theme, density))
            val neverTv = createItemView("Learn \"$word\"", isBold = false, isAccent = true, theme = theme, density = density) {
                onNeverAutocorrectListener?.invoke(word)
                dismiss()
            }
            containerLayout.addView(neverTv)
        }

        try {
            popupWindow = PopupWindow(
                containerLayout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
            ).apply {
                isFocusable = false
                isOutsideTouchable = true
                elevation = 16f * density
                animationStyle = android.R.style.Animation_Toast
            }

            val yOffset = anchorView.height + (8 * density).toInt()
            popupWindow?.showAtLocation(
                anchorView,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,
                yOffset
            )
        } catch (_: Throwable) {}
    }

    fun isShowing(): Boolean = popupWindow?.isShowing == true

    fun dismiss() {
        try {
            popupWindow?.dismiss()
        } catch (_: Throwable) {}
        popupWindow = null
        containerLayout.removeAllViews()
    }

    private fun createItemView(
        text: String,
        isBold: Boolean,
        isAccent: Boolean,
        theme: ThemeColors,
        density: Float,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            maxLines = 1
            val padH = (10 * density).toInt()
            val padV = (5 * density).toInt()
            setPadding(padH, padV, padH, padV)
            setTextColor(
                when {
                    isAccent -> theme.accentBlue
                    else -> theme.textPrimary
                }
            )
            typeface = if (isBold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            setOnClickListener { onClick() }
        }
    }

    private fun createDivider(theme: ThemeColors, density: Float): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (1f * density).toInt().coerceAtLeast(1),
                (18 * density).toInt()
            ).apply {
                val m = (2 * density).toInt()
                setMargins(m, 0, m, 0)
            }
            setBackgroundColor(if (theme.isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#20000000"))
        }
    }
}
