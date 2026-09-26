package org.iosclone.keyboard.writingtools

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import org.iosclone.keyboard.theme.ThemeColors

/**
 * Modern iOS Apple Intelligence Writing Tools UI bottom sheet.
 * Features signature iridescent glowing border, proofreading,
 * tone rewriting, summarization, and Genmoji creator.
 */
class WritingToolsBottomSheet(
    private val context: Context,
    private val theme: ThemeColors,
    private val currentText: String,
    private val onReplaceText: (String) -> Unit
) {

    private val engine = WritingToolsEngine()
    private var popupWindow: PopupWindow? = null

    fun show(anchorView: View) {
        val density = context.resources.displayMetrics.density
        val popupHeight = (320 * density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#E61C1C1E") else Color.parseColor("#F5F7F9"))
                cornerRadius = 16f * density
                // Apple Intelligence iridescent blue-purple border
                setStroke((2 * density).toInt(), Color.parseColor("#997B61FF"))
            }
            background = bg
            elevation = 24f * density
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                popupHeight
            )
        }

        // Header: Apple Intelligence ✨ + Done
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (38 * density).toInt())
        }

        val titleTv = TextView(context).apply {
            text = "✨ Writing Tools"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.textPrimary)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val doneBtn = TextView(context).apply {
            text = "Done"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.accentBlue)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            setOnClickListener { popupWindow?.dismiss() }
        }

        header.addView(titleTv)
        header.addView(doneBtn)
        root.addView(header)

        // Action Pill Buttons Row (Scrollable)
        val pillScrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val pillRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val resultArea = TextView(context).apply {
            text = if (currentText.isBlank()) "Type or select text first to use Writing Tools." else currentText
            textSize = 15f
            setTextColor(theme.textPrimary)
            val p = (10 * density).toInt()
            setPadding(p, p, p, p)
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) 0x22FFFFFF else 0x14000000)
                cornerRadius = 10f * density
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val replaceBtn = TextView(context).apply {
            text = "Replace Text"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(theme.accentBlue)
                cornerRadius = 10f * density
            }
            val padV = (10 * density).toInt()
            setPadding(0, padV, 0, padV)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (42 * density).toInt()).apply {
                topMargin = (10 * density).toInt()
            }
            visibility = View.GONE
            setOnClickListener {
                onReplaceText(resultArea.text.toString())
                popupWindow?.dismiss()
            }
        }

        fun updateResult(newText: String) {
            resultArea.text = newText
            replaceBtn.visibility = if (newText != currentText && newText.isNotBlank()) View.VISIBLE else View.GONE
        }

        // Action Pills
        pillRow.addView(createActionPill("✍️ Proofread", density) {
            val res = engine.proofread(currentText)
            updateResult(res.correctedText)
        })

        pillRow.addView(createActionPill("😊 Friendly", density) {
            updateResult(engine.rewrite(currentText, WritingToolsEngine.Tone.FRIENDLY))
        })

        pillRow.addView(createActionPill("💼 Professional", density) {
            updateResult(engine.rewrite(currentText, WritingToolsEngine.Tone.PROFESSIONAL))
        })

        pillRow.addView(createActionPill("⚡ Concise", density) {
            updateResult(engine.rewrite(currentText, WritingToolsEngine.Tone.CONCISE))
        })

        pillRow.addView(createActionPill("📋 Key Points", density) {
            updateResult(engine.summarize(currentText, WritingToolsEngine.SummaryStyle.KEY_POINTS))
        })

        pillRow.addView(createActionPill("🎨 Genmoji", density) {
            val gen = engine.createGenmoji(if (currentText.isBlank()) "party celebration" else currentText)
            updateResult("${gen.primaryEmoji}${gen.secondaryEmoji} (${gen.compositeDescription})")
        })

        pillScrollView.addView(pillRow)
        root.addView(pillScrollView)

        // Scrollable Preview
        val previewScroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f)
            isVerticalScrollBarEnabled = true
        }
        previewScroll.addView(resultArea)
        root.addView(previewScroll)

        root.addView(replaceBtn)

        popupWindow = PopupWindow(
            root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            popupHeight,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = 25f * density
        }

        popupWindow?.showAtLocation(anchorView, Gravity.BOTTOM, 0, 0)
    }

    private fun createActionPill(title: String, density: Float, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = title
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.textPrimary)
            val padH = (12 * density).toInt()
            val padV = (6 * density).toInt()
            setPadding(padH, padV, padH, padV)
            val marginH = (4 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(marginH, 0, marginH, 0)
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA"))
                cornerRadius = 14f * density
            }
            setOnClickListener { onClick() }
        }
    }
}
