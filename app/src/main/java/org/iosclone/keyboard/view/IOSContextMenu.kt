package org.iosclone.keyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import org.iosclone.keyboard.layout.LanguageLayout
import org.iosclone.keyboard.layout.OneHandedMode
import org.iosclone.keyboard.theme.ThemeColors

/**
 * Authentic iOS Globe / Emoji long-press context menu popup.
 * Displays keyboard settings, language list with active checkmark,
 * emoji shortcut, and one-handed keyboard dock controls.
 */
class IOSContextMenu(
    private val context: Context,
    private val currentTheme: ThemeColors,
    private val activeLanguage: LanguageLayout,
    private val currentOneHandedMode: OneHandedMode,
    private val onLanguageSelected: (LanguageLayout) -> Unit,
    private val onEmojiSelected: () -> Unit,
    private val onSettingsSelected: () -> Unit,
    private val onOneHandedSelected: (OneHandedMode) -> Unit
) {

    private var popupWindow: PopupWindow? = null

    fun show(anchorView: View, anchorX: Float, anchorY: Float) {
        val density = context.resources.displayMetrics.density
        val popupWidth = (240 * density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(if (currentTheme.isDark) Color.parseColor("#E62C2C2E") else Color.parseColor("#F5F7F8"))
                cornerRadius = 14f * density
                setStroke((1 * density).toInt(), if (currentTheme.isDark) 0x33FFFFFF else 0x22000000)
            }
            background = bg
            elevation = 16f * density
            val pad = (6 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // 1. Keyboard Settings...
        val settingsRow = createMenuItem("Keyboard Settings…", isAction = true) {
            popupWindow?.dismiss()
            onSettingsSelected()
        }
        root.addView(settingsRow)

        root.addView(createDivider(density))

        // 2. Languages list
        val supportedLanguages = listOf(
            LanguageLayout.QWERTY,
            LanguageLayout.SPANISH,
            LanguageLayout.AZERTY,
            LanguageLayout.QWERTZ
        )

        for (lang in supportedLanguages) {
            val isSelected = (lang == activeLanguage)
            val langRow = createMenuItem(
                title = lang.displayName,
                isSelected = isSelected,
                isAction = false
            ) {
                popupWindow?.dismiss()
                onLanguageSelected(lang)
            }
            root.addView(langRow)
        }

        // 3. Emoji shortcut
        val emojiRow = createMenuItem("Emoji", isAction = false) {
            popupWindow?.dismiss()
            onEmojiSelected()
        }
        root.addView(emojiRow)

        root.addView(createDivider(density))

        // 4. One-handed dock switcher
        val dockContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (42 * density).toInt()
            )
        }

        val btnLeft = createDockButton("ic_onehand_left", currentOneHandedMode == OneHandedMode.LEFT_DOCKED, density) {
            popupWindow?.dismiss()
            onOneHandedSelected(OneHandedMode.LEFT_DOCKED)
        }
        val btnCenter = createDockCenterButton(currentOneHandedMode == OneHandedMode.NORMAL, density) {
            popupWindow?.dismiss()
            onOneHandedSelected(OneHandedMode.NORMAL)
        }
        val btnRight = createDockButton("ic_onehand_right", currentOneHandedMode == OneHandedMode.RIGHT_DOCKED, density) {
            popupWindow?.dismiss()
            onOneHandedSelected(OneHandedMode.RIGHT_DOCKED)
        }

        dockContainer.addView(btnLeft)
        dockContainer.addView(btnCenter)
        dockContainer.addView(btnRight)
        root.addView(dockContainer)

        popupWindow = PopupWindow(
            root,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = 20f * density
        }

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        val x = location[0] + (12 * density).toInt()
        val y = location[1] - (310 * density).toInt()

        popupWindow?.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
    }

    private fun createMenuItem(
        title: String,
        isSelected: Boolean = false,
        isAction: Boolean = false,
        onClick: () -> Unit
    ): LinearLayout {
        val density = context.resources.displayMetrics.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (12 * density).toInt()
            val padV = (9 * density).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            if (isSelected) {
                background = GradientDrawable().apply {
                    setColor(if (currentTheme.isDark) 0x33FFFFFF else 0x1A000000)
                    cornerRadius = 8f * density
                }
            }

            val checkMark = TextView(context).apply {
                text = if (isSelected) "✓ " else "   "
                textSize = 15f
                setTextColor(currentTheme.accentBlue)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            if (!isAction) {
                addView(checkMark)
            }

            val tv = TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(if (isAction) currentTheme.accentBlue else currentTheme.textPrimary)
                typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            }
            addView(tv)

            setOnClickListener { onClick() }
        }
    }

    private fun createDivider(density: Float): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt().coerceAtLeast(1)
            ).apply {
                val m = (4 * density).toInt()
                setMargins(m, m, m, m)
            }
            setBackgroundColor(if (currentTheme.isDark) 0x22FFFFFF else 0x18000000)
        }
    }

    private fun createDockButton(iconName: String, isSelected: Boolean, density: Float, onClick: () -> Unit): ImageView {
        return ImageView(context).apply {
            val size = (38 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                val m = (8 * density).toInt()
                setMargins(m, 0, m, 0)
            }
            val pad = (7 * density).toInt()
            setPadding(pad, pad, pad, pad)
            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resId != 0) setImageResource(resId)
            setColorFilter(if (isSelected) currentTheme.accentBlue else currentTheme.textSecondary)

            if (isSelected) {
                background = GradientDrawable().apply {
                    setColor(if (currentTheme.isDark) 0x33FFFFFF else 0x1A000000)
                    cornerRadius = 8f * density
                }
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createDockCenterButton(isSelected: Boolean, density: Float, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            val size = (38 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                val m = (8 * density).toInt()
                setMargins(m, 0, m, 0)
            }
            text = "⌨"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(if (isSelected) currentTheme.accentBlue else currentTheme.textSecondary)

            if (isSelected) {
                background = GradientDrawable().apply {
                    setColor(if (currentTheme.isDark) 0x33FFFFFF else 0x1A000000)
                    cornerRadius = 8f * density
                }
            }
            setOnClickListener { onClick() }
        }
    }
}
