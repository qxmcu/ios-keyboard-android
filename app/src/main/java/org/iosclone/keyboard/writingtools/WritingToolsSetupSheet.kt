package org.iosclone.keyboard.writingtools

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.iosclone.keyboard.settings.KeyboardPreferences
import org.iosclone.keyboard.theme.ThemeColors
import org.iosclone.keyboard.view.AppleIntelligenceIconView

/**
 * First-time setup and reminder bottom sheet for Apple Intelligence Writing Tools.
 * Prompts user for Google Gemini API key, supports "Set Up Later", and displays
 * "You haven't set up Writing Tools" reminder with icon and dismiss button.
 */
class WritingToolsSetupSheet(
    private val context: Context,
    private val theme: ThemeColors,
    private val preferences: KeyboardPreferences,
    private val onSetupCompleted: () -> Unit
) {

    private var overlayLayout: FrameLayout? = null

    fun show(parent: ViewGroup, isReminder: Boolean = false) {
        val density = context.resources.displayMetrics.density

        val overlay = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
            isClickable = true
            isFocusable = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { dismiss() }
        }
        overlayLayout = overlay

        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padH = (20 * density).toInt()
            val padV = (16 * density).toInt()
            setPadding(padH, padV, padH, padV)
            isClickable = true
            isFocusable = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            background = GradientDrawable().apply {
                val r = 24f * density
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setColor(if (theme.isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#F2F2F7"))
            }
        }

        // Drag handle
        val dragHandle = View(context).apply {
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (4.5f * density).toInt()).apply {
                bottomMargin = (14 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#545458") else Color.parseColor("#D1D1D6"))
                cornerRadius = 2.5f * density
            }
        }
        sheet.addView(dragHandle)

        // Apple Intelligence Icon
        val aiIcon = AppleIntelligenceIconView(context).apply {
            val s = (44 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                bottomMargin = (12 * density).toInt()
            }
            setTheme(theme.isDark)
        }
        sheet.addView(aiIcon)

        if (isReminder) {
            buildReminderContent(sheet, density)
        } else {
            buildSetupContent(sheet, density)
        }

        overlay.addView(sheet)
        parent.addView(overlay)
    }

    private fun buildReminderContent(sheet: LinearLayout, density: Float) {
        val titleTv = TextView(context).apply {
            text = "You haven't set up Writing Tools."
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(theme.textPrimary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (6 * density).toInt()
            }
        }

        val descTv = TextView(context).apply {
            text = "Enter a Google Gemini API key to activate Apple-style Proofreading, Rewriting, and Summarizing powered by Gemini Flash-Lite."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(theme.textSecondary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (18 * density).toInt()
            }
        }

        sheet.addView(titleTv)
        sheet.addView(descTv)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val dismissBtn = TextView(context).apply {
            text = "Dismiss"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (theme.isDark) Color.parseColor("#AEAEB2") else Color.parseColor("#636366"))
            layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1.0f).apply {
                marginEnd = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA"))
                cornerRadius = 12f * density
            }
            setOnClickListener { dismiss() }
        }

        val setupNowBtn = TextView(context).apply {
            text = "Set Up Now"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1.0f).apply {
                marginStart = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(theme.accentBlue)
                cornerRadius = 12f * density
            }
            setOnClickListener {
                sheet.removeAllViews()
                val dragHandle = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (4.5f * density).toInt()).apply {
                        bottomMargin = (14 * density).toInt()
                    }
                    background = GradientDrawable().apply {
                        setColor(if (theme.isDark) Color.parseColor("#545458") else Color.parseColor("#D1D1D6"))
                        cornerRadius = 2.5f * density
                    }
                }
                sheet.addView(dragHandle)
                val newIcon = AppleIntelligenceIconView(context).apply {
                    val s = (44 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(s, s).apply {
                        bottomMargin = (12 * density).toInt()
                    }
                    setTheme(theme.isDark)
                }
                sheet.addView(newIcon)
                buildSetupContent(sheet, density)
            }
        }

        btnRow.addView(dismissBtn)
        btnRow.addView(setupNowBtn)
        sheet.addView(btnRow)
    }

    private fun buildSetupContent(sheet: LinearLayout, density: Float) {
        val titleTv = TextView(context).apply {
            text = "Set Up Writing Tools"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(theme.textPrimary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (6 * density).toInt()
            }
        }

        val descTv = TextView(context).apply {
            text = "Powered by Google Gemini Flash-Lite for sub-second, battery-efficient writing intelligence. Paste your free Gemini API key below:"
            textSize = 13.5f
            gravity = Gravity.CENTER
            setTextColor(theme.textSecondary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * density).toInt()
            }
        }

        sheet.addView(titleTv)
        sheet.addView(descTv)

        val inputContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (44 * density).toInt()).apply {
                bottomMargin = (10 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#2C2C2E") else Color.WHITE)
                cornerRadius = 12f * density
                setStroke((1 * density).toInt().coerceAtLeast(1), if (theme.isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#D1D1D6"))
            }
        }

        val apiKeyEt = EditText(context).apply {
            hint = "Paste Gemini API Key (AIzaSy...)"
            setHintTextColor(if (theme.isDark) Color.parseColor("#636366") else Color.parseColor("#8E8E93"))
            setTextColor(theme.textPrimary)
            textSize = 14f
            background = null
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            val p = (12 * density).toInt()
            setPadding(p, 0, p, 0)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            if (preferences.geminiApiKey.isNotBlank()) {
                setText(preferences.geminiApiKey)
            }
        }
        inputContainer.addView(apiKeyEt)
        sheet.addView(inputContainer)

        val getKeyLink = TextView(context).apply {
            text = "Get a free API key at ai.google.dev"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(theme.accentBlue)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
            setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
        sheet.addView(getKeyLink)

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val laterBtn = TextView(context).apply {
            text = "Set Up Later"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (theme.isDark) Color.parseColor("#AEAEB2") else Color.parseColor("#636366"))
            layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1.0f).apply {
                marginEnd = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(if (theme.isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA"))
                cornerRadius = 12f * density
            }
            setOnClickListener {
                preferences.writingToolsSetupDeclined = true
                dismiss()
            }
        }

        val enableBtn = TextView(context).apply {
            text = "Enable Writing Tools"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1.25f).apply {
                marginStart = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(theme.accentBlue)
                cornerRadius = 12f * density
            }
            setOnClickListener {
                val entered = apiKeyEt.text.toString().trim()
                if (entered.isBlank()) {
                    Toast.makeText(context, "Please enter an API Key", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                preferences.geminiApiKey = entered
                preferences.writingToolsSetupDeclined = false
                dismiss()
                onSetupCompleted()
            }
        }

        actionRow.addView(laterBtn)
        actionRow.addView(enableBtn)
        sheet.addView(actionRow)
    }

    fun dismiss() {
        val overlay = overlayLayout ?: return
        val parent = overlay.parent as? ViewGroup
        parent?.removeView(overlay)
        overlayLayout = null
    }
}
