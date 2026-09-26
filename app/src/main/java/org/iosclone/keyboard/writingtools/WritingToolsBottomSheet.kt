package org.iosclone.keyboard.writingtools

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.iosclone.keyboard.theme.ThemeColors
import org.iosclone.keyboard.view.AppleIntelligenceIconView

/**
 * Pixel-perfect iOS 18 Apple Intelligence Writing Tools UI bottom sheet.
 * Features:
 * - Top drag handle
 * - "Writing Tools" title + 1-tap instant circular (X) close button (no 2-tap glitch)
 * - "Describe your change" prompt pill with iridescent Siri emblem
 * - 2 large action cards: Proofread & Rewrite
 * - 3 tone cards: Friendly, Professional, Concise
 * - 4 format cards: Summary, Key Points, List, Table
 * - Compose card
 * - Result preview with Apple Intelligence iridescent glowing border and 1-tap Replace
 */
class WritingToolsBottomSheet(
    private val context: Context,
    private val theme: ThemeColors,
    private val currentText: String,
    private val onReplaceText: (String) -> Unit
) {

    private val engine = WritingToolsEngine()
    private var containerView: FrameLayout? = null
    private var sheetLayout: LinearLayout? = null
    private var isDismissing = false

    fun show(parentContainer: ViewGroup) {
        val density = context.resources.displayMetrics.density

        // Full scrim + sheet overlay
        val overlay = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(if (theme.isDark) Color.parseColor("#77000000") else Color.parseColor("#44000000"))
            isClickable = true
            isFocusable = true
            setOnClickListener { dismiss() }
        }
        containerView = overlay

        // Floating rounded bottom card
        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            val bg = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#E61C1C1E") else Color.parseColor("#F5F5F7"))
                cornerRadii = floatArrayOf(
                    26f * density, 26f * density, // top-left
                    26f * density, 26f * density, // top-right
                    0f, 0f, // bottom-right
                    0f, 0f  // bottom-left
                )
                setStroke((1 * density).toInt().coerceAtLeast(1), if (theme.isDark) Color.parseColor("#38383A") else Color.parseColor("#D1D1D6"))
            }
            background = bg
            elevation = 20f * density
            val pad = (14 * density).toInt()
            setPadding(pad, (8 * density).toInt(), pad, pad)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        }
        sheetLayout = sheet

        // 1. Drag Handle
        val dragHandle = View(context).apply {
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (4.5f * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (10 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#545458") else Color.parseColor("#D1D1D6"))
                cornerRadius = 2.5f * density
            }
        }
        sheet.addView(dragHandle)

        // 2. Header: Title + Circular Close (X) Button
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (34 * density).toInt()).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val titleTv = TextView(context).apply {
            text = "Writing Tools"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.textPrimary)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (theme.isDark) Color.parseColor("#AEAEB2") else Color.parseColor("#636366"))
            val btnSize = (28 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (theme.isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA"))
            }
            // Fix 2-tap glitch: Immediate single-tap dismiss!
            setOnClickListener { dismiss() }
        }

        header.addView(titleTv)
        header.addView(closeBtn)
        sheet.addView(header)

        // 3. Prompt Input Pill ("Describe your change")
        val inputPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val h = (42 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h).apply {
                bottomMargin = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#2C2C2E") else Color.WHITE)
                cornerRadius = 21f * density
                setStroke((1 * density).toInt().coerceAtLeast(1), if (theme.isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA"))
            }
            val p = (8 * density).toInt()
            setPadding(p, 0, p, 0)
        }

        val aiIcon = AppleIntelligenceIconView(context).apply {
            val s = (28 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                marginEnd = (8 * density).toInt()
            }
            setTheme(theme.isDark)
        }

        val promptEt = EditText(context).apply {
            hint = "Describe your change"
            setHintTextColor(if (theme.isDark) Color.parseColor("#636366") else Color.parseColor("#8E8E93"))
            setTextColor(theme.textPrimary)
            textSize = 15f
            background = null
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
        }

        inputPill.addView(aiIcon)
        inputPill.addView(promptEt)
        sheet.addView(inputPill)

        // Scrollable content area for options & results
        val scrollContent = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (220 * density).toInt())
        }

        val toolsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        // Result Card (Initially hidden)
        val resultCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            val p = (12 * density).toInt()
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#242426") else Color.WHITE)
                cornerRadius = 14f * density
                // Apple Intelligence signature iridescent glowing border
                setStroke((1.5f * density).toInt().coerceAtLeast(1), Color.parseColor("#997B61FF"))
            }
        }

        val resultTv = TextView(context).apply {
            textSize = 14.5f
            setTextColor(theme.textPrimary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val resultBtnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val copyBtn = TextView(context).apply {
            text = "Copy"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(theme.accentBlue)
            val btnPadH = (14 * density).toInt()
            val btnPadV = (7 * density).toInt()
            setPadding(btnPadH, btnPadV, btnPadH, btnPadV)
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA"))
                cornerRadius = 12f * density
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (8 * density).toInt()
            }
            setOnClickListener {
                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clip?.setPrimaryClip(ClipData.newPlainText("Writing Tools", resultTv.text.toString()))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        val replaceBtn = TextView(context).apply {
            text = "Replace"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            val btnPadH = (18 * density).toInt()
            val btnPadV = (7 * density).toInt()
            setPadding(btnPadH, btnPadV, btnPadH, btnPadV)
            background = GradientDrawable().apply {
                setColor(theme.accentBlue)
                cornerRadius = 12f * density
            }
            setOnClickListener {
                onReplaceText(resultTv.text.toString())
                dismiss()
            }
        }

        resultBtnRow.addView(copyBtn)
        resultBtnRow.addView(replaceBtn)
        resultCard.addView(resultTv)
        resultCard.addView(resultBtnRow)
        toolsContainer.addView(resultCard)

        fun displayResult(newText: String) {
            resultTv.text = newText
            resultCard.visibility = View.VISIBLE
            scrollContent.smoothScrollTo(0, 0)
        }

        promptEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val q = promptEt.text.toString().trim()
                if (q.isNotEmpty()) {
                    displayResult(engine.customTransform(currentText, q))
                }
                true
            } else false
        }

        // 4. Row 1: Two Large Action Cards (Proofread & Rewrite)
        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }
        row1.addView(createCard("🔍✨", "Proofread", 1.0f, density) {
            displayResult(engine.proofread(currentText).correctedText)
        })
        val gap1 = View(context).apply { layoutParams = LinearLayout.LayoutParams((8 * density).toInt(), 1) }
        row1.addView(gap1)
        row1.addView(createCard("🔄✨", "Rewrite", 1.0f, density) {
            displayResult(engine.rewrite(currentText, WritingToolsEngine.Tone.FRIENDLY))
        })
        toolsContainer.addView(row1)

        // 5. Row 2: Three Tone Cards (Friendly, Professional, Concise)
        val row2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }
        row2.addView(createCard("☺", "Friendly", 1.0f, density) {
            displayResult(engine.rewrite(currentText, WritingToolsEngine.Tone.FRIENDLY))
        })
        val gap2a = View(context).apply { layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1) }
        row2.addView(gap2a)
        row2.addView(createCard("💼", "Professional", 1.0f, density) {
            displayResult(engine.rewrite(currentText, WritingToolsEngine.Tone.PROFESSIONAL))
        })
        val gap2b = View(context).apply { layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1) }
        row2.addView(gap2b)
        row2.addView(createCard("⇋", "Concise", 1.0f, density) {
            displayResult(engine.rewrite(currentText, WritingToolsEngine.Tone.CONCISE))
        })
        toolsContainer.addView(row2)

        // 6. Row 3: Four Format Cards (Summary, Key Points, List, Table)
        val row3 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }
        row3.addView(createMiniCard("📄↓", "Summary", 1.0f, density) {
            displayResult(engine.summarize(currentText, WritingToolsEngine.SummaryStyle.TLDR))
        })
        val gap3a = View(context).apply { layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1) }
        row3.addView(gap3a)
        row3.addView(createMiniCard("📋↓", "Key Points", 1.0f, density) {
            displayResult(engine.summarize(currentText, WritingToolsEngine.SummaryStyle.KEY_POINTS))
        })
        val gap3b = View(context).apply { layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1) }
        row3.addView(gap3b)
        row3.addView(createMiniCard("☰", "List", 1.0f, density) {
            displayResult(engine.formatList(currentText))
        })
        val gap3c = View(context).apply { layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1) }
        row3.addView(gap3c)
        row3.addView(createMiniCard("▦", "Table", 1.0f, density) {
            displayResult(engine.formatTable(currentText))
        })
        toolsContainer.addView(row3)

        // 7. Row 4: Full-width Compose Pill Card
        val composeCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val h = (42 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h).apply {
                bottomMargin = (4 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#EBEBF0"))
                cornerRadius = 14f * density
            }
            val p = (14 * density).toInt()
            setPadding(p, 0, p, 0)
            setOnClickListener {
                displayResult(engine.composeText(currentText))
            }
        }

        val composeIcon = TextView(context).apply {
            text = "✎"
            textSize = 15f
            setTextColor(theme.textPrimary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (8 * density).toInt()
            }
        }

        val composeTv = TextView(context).apply {
            text = "Compose"
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.textPrimary)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val chevronTv = TextView(context).apply {
            text = "›"
            textSize = 18f
            setTextColor(if (theme.isDark) Color.parseColor("#8E8E93") else Color.parseColor("#AEAEB2"))
        }

        composeCard.addView(composeIcon)
        composeCard.addView(composeTv)
        composeCard.addView(chevronTv)
        toolsContainer.addView(composeCard)

        scrollContent.addView(toolsContainer)
        sheet.addView(scrollContent)
        overlay.addView(sheet)

        // Smooth slide-up presentation animation
        parentContainer.addView(overlay)
        sheet.translationY = 500f * density
        sheet.alpha = 0.5f
        sheet.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun createCard(icon: String, title: String, weight: Float, density: Float, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, (52 * density).toInt(), weight)
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#EBEBF0"))
                cornerRadius = 14f * density
            }
            setOnClickListener { onClick() }

            val iconTv = TextView(context).apply {
                text = icon
                textSize = 15f
                gravity = Gravity.CENTER
            }
            val labelTv = TextView(context).apply {
                text = title
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(theme.textPrimary)
                gravity = Gravity.CENTER
            }
            addView(iconTv)
            addView(labelTv)
        }
    }

    private fun createMiniCard(icon: String, title: String, weight: Float, density: Float, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, (54 * density).toInt(), weight)
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#EBEBF0"))
                cornerRadius = 12f * density
            }
            setOnClickListener { onClick() }

            val iconTv = TextView(context).apply {
                text = icon
                textSize = 15f
                gravity = Gravity.CENTER
            }
            val labelTv = TextView(context).apply {
                text = title
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(theme.textPrimary)
                gravity = Gravity.CENTER
            }
            addView(iconTv)
            addView(labelTv)
        }
    }

    fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        val sheet = sheetLayout
        val container = containerView
        if (sheet != null && container != null) {
            sheet.animate()
                .translationY(sheet.height.toFloat().coerceAtLeast(300f))
                .alpha(0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        (container.parent as? ViewGroup)?.removeView(container)
                    }
                })
                .start()
        } else {
            (container?.parent as? ViewGroup)?.removeView(container)
        }
    }
}
