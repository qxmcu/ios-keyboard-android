package org.iosclone.keyboard.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.theme.ThemeColors

/**
 * Renders the authentic iOS balloon magnifier popup bubble
 * directly above the finger on key touch down.
 */
class KeyMagnifierPopup {

    private val bubblePath = Path()
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    fun draw(
        canvas: Canvas,
        key: KeyDefinition,
        theme: ThemeColors,
        density: Float
    ) {
        if (key.isModifier() || key.label.length > 2) return

        val bounds = key.bounds
        val bubbleWidth = bounds.width() * 1.35f
        val bubbleHeight = bounds.height() * 1.30f
        val cornerRadius = 12f * density

        val bubbleCenterX = bounds.centerX()
        val bubbleTop = bounds.top - bubbleHeight + (6f * density)
        val bubbleBottom = bounds.top + (4f * density)
        val bubbleLeft = bubbleCenterX - (bubbleWidth / 2f)
        val bubbleRight = bubbleCenterX + (bubbleWidth / 2f)

        bubblePath.reset()
        // Upper rounded bubble
        val upperRect = RectF(bubbleLeft, bubbleTop, bubbleRight, bounds.top)
        bubblePath.addRoundRect(upperRect, cornerRadius, cornerRadius, Path.Direction.CW)

        // Connecting neck down to key surface
        bubblePath.moveTo(bubbleLeft + (8f * density), bounds.top - (2f * density))
        bubblePath.quadTo(
            bubbleLeft + (12f * density),
            bounds.top + (4f * density),
            bounds.left,
            bounds.top + (8f * density)
        )
        bubblePath.lineTo(bounds.right, bounds.top + (8f * density))
        bubblePath.quadTo(
            bubbleRight - (12f * density),
            bounds.top + (4f * density),
            bubbleRight - (8f * density),
            bounds.top - (2f * density)
        )
        bubblePath.close()

        // Drop shadow for elevated iOS popup
        shadowPaint.color = theme.popupShadow
        canvas.save()
        canvas.translate(0f, 3f * density)
        canvas.drawPath(bubblePath, shadowPaint)
        canvas.restore()

        // Bubble surface
        bubblePaint.color = theme.popupBackground
        canvas.drawPath(bubblePath, bubblePaint)

        // Large magnified character label
        textPaint.color = theme.textPrimary
        textPaint.textSize = bounds.height() * 0.72f
        val fontMetrics = textPaint.fontMetrics
        val textY = upperRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(key.label, bubbleCenterX, textY, textPaint)
    }
}
