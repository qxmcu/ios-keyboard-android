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
        visibility = View.GONE
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
        visibility = View.VISIBLE
        invalidate()
    }

    fun hideMagnifier() {
        if (!isLongPress) {
            activeKey = null
            visibility = View.GONE
            invalidate()
        }
    }

    fun showLongPress(key: KeyDefinition, originView: IOSKeyboardView) {
        this.keyboardView = originView
        this.activeKey = key
        this.isLongPress = true
        visibility = View.VISIBLE
        invalidate()
    }

    fun updateLongPressSelection(touchX: Float) {
        if (isLongPress && activeKey != null) {
            longPressPopup.updateSelection(touchX)
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
        visibility = View.GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val key = activeKey ?: return
        val origin = keyboardView ?: return
        val density = resources.displayMetrics.density

        // Compute origin offset relative to overlay view
        val originLoc = IntArray(2)
        origin.getLocationInWindow(originLoc)
        val overlayLoc = IntArray(2)
        getLocationInWindow(overlayLoc)

        val offsetX = (originLoc[0] - overlayLoc[0]).toFloat()
        val offsetY = (originLoc[1] - overlayLoc[1]).toFloat()

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
