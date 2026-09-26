package org.iosclone.keyboard.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.theme.ThemeColors

class LongPressPopup {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val popupBounds = RectF()
    private val itemBounds = mutableListOf<RectF>()

    var selectedIndex: Int = -1
        private set

    var hasDraggedToSelection: Boolean = false
        private set

    fun isShowing(): Boolean = itemBounds.isNotEmpty()

    fun updateSelection(touchX: Float) {
        if (itemBounds.isEmpty()) return
        for (i in itemBounds.indices) {
            val r = itemBounds[i]
            if (touchX >= r.left && touchX <= r.right) {
                selectedIndex = i
                hasDraggedToSelection = true
                return
            }
        }
        // If outside left/right bounds, clamp if user has moved horizontally
        if (touchX < popupBounds.left) {
            selectedIndex = 0
            hasDraggedToSelection = true
        } else if (touchX > popupBounds.right) {
            selectedIndex = itemBounds.lastIndex
            hasDraggedToSelection = true
        }
    }

    fun getSelectedCharacter(key: KeyDefinition): String? {
        val accents = key.accents
        if (accents.isEmpty() || !hasDraggedToSelection || selectedIndex !in accents.indices) {
            return null
        }
        return accents.getOrNull(selectedIndex)
    }

    fun dismiss() {
        itemBounds.clear()
        popupBounds.setEmpty()
        selectedIndex = -1
        hasDraggedToSelection = false
    }

    fun draw(
        canvas: Canvas,
        key: KeyDefinition,
        viewWidth: Float,
        theme: ThemeColors,
        density: Float
    ) {
        val accents = key.accents
        if (accents.isEmpty()) return

        val itemWidth = 38f * density
        val itemHeight = 44f * density
        val totalWidth = accents.size * itemWidth
        val cornerRadius = 10f * density

        val keyBounds = key.bounds
        var left = keyBounds.centerX() - (totalWidth / 2f)
        // Clamp to screen bounds
        if (left < 8f * density) left = 8f * density
        if (left + totalWidth > viewWidth - (8f * density)) {
            left = viewWidth - (8f * density) - totalWidth
        }

        val top = keyBounds.top - itemHeight - (10f * density)
        val right = left + totalWidth
        val bottom = top + itemHeight

        popupBounds.set(left, top, right, bottom)

        // Drop shadow
        shadowPaint.color = theme.popupShadow
        canvas.drawRoundRect(
            RectF(left, top + (2f * density), right, bottom + (2f * density)),
            cornerRadius, cornerRadius, shadowPaint
        )

        // Background
        bgPaint.color = theme.popupBackground
        canvas.drawRoundRect(popupBounds, cornerRadius, cornerRadius, bgPaint)

        // Layout items
        itemBounds.clear()
        var currentX = left

        for (i in accents.indices) {
            val rect = RectF(currentX, top, currentX + itemWidth, bottom)
            itemBounds.add(rect)

            // Draw highlight if selected
            if (hasDraggedToSelection && i == selectedIndex) {
                highlightPaint.color = theme.accentBlue
                canvas.drawRoundRect(
                    RectF(rect.left + (2f * density), rect.top + (2f * density), rect.right - (2f * density), rect.bottom - (2f * density)),
                    8f * density, 8f * density, highlightPaint
                )
            }

            // Draw character
            textPaint.color = if (hasDraggedToSelection && i == selectedIndex) 0xFFFFFFFF.toInt() else theme.textPrimary
            textPaint.textSize = 20f * density
            val fontMetrics = textPaint.fontMetrics
            val textY = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(accents[i], rect.centerX(), textY, textPaint)

            currentX += itemWidth
        }
    }
}
