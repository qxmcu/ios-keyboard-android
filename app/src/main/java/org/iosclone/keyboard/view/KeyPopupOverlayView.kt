package org.iosclone.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.view.View
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.theme.ThemeColors

/**
 * Top-level overlay view that spans across the entire keyboard layout,
 * including above the suggestion strip.
 * Renders key magnifier balloons and long-press accent popups without
 * any view clipping or obstruction from sibling views.
 */
class KeyPopupOverlayView(context: Context) : View(context) {

    private val magnifierPopup = KeyMagnifierPopup()
    private val longPressPopup = LongPressPopup()

    private var activeKey: KeyDefinition? = null
    private var isLongPress = false
    private var currentTheme: ThemeColors = ThemeColors.Light
    private var keyboardView: IOSKeyboardView? = null

    init {
        isClickable = false
        isFocusable = false
        visibility = View.VISIBLE
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setWillNotDraw(false)
    }

    fun setTheme(theme: ThemeColors) {
        this.currentTheme = theme
        invalidate()
    }

    fun showMagnifier(key: KeyDefinition, originView: IOSKeyboardView) {
        this.keyboardView = originView
        this.activeKey = key
        this.isLongPress = false
        magnifierPopup.onKeyDown(key)
        invalidate()
    }

    fun hideMagnifier() {
        if (!isLongPress) {
            activeKey = null
            invalidate()
        }
    }

    fun showLongPress(key: KeyDefinition, originView: IOSKeyboardView) {
        this.keyboardView = originView
        this.activeKey = key
        this.isLongPress = true
        invalidate()
    }

    fun updateLongPressSelection(touchX: Float, touchY: Float = Float.MAX_VALUE) {
        val key = activeKey
        if (isLongPress && key != null) {
            longPressPopup.updateSelection(touchX, touchY, key)
            invalidate()
        }
    }

    fun getSelectedAccent(key: KeyDefinition): String? {
        return if (isLongPress) longPressPopup.getSelectedCharacter(key) else null
    }

    fun hideLongPress() {
        isLongPress = false
        activeKey = null
        longPressPopup.dismiss()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val key = activeKey ?: return
        val origin = keyboardView ?: return
        val density = resources.displayMetrics.density

        // Compute local relative coordinate offset directly without WindowManager IPC
        var offsetX = 0f
        var offsetY = 0f
        var v: View? = origin
        while (v != null && v !== this.parent && v !== this) {
            offsetX += v.x
            offsetY += v.y
            v = v.parent as? View
        }

        canvas.save()
        canvas.translate(offsetX, offsetY)

        if (isLongPress && key.accents.isNotEmpty()) {
            longPressPopup.draw(canvas, key, origin.width.toFloat(), currentTheme, density)
        } else {
            magnifierPopup.draw(canvas, key, currentTheme, density, origin.width.toFloat())
        }

        canvas.restore()
    }
}
