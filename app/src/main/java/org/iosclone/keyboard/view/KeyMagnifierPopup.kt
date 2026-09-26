package org.iosclone.keyboard.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.theme.ThemeColors

/**
 * Renders the authentic iOS balloon magnifier popup bubble
 * directly above the finger on key touch down.
 * Features edge-clamping to prevent clipping on border keys (Q, P, 1, 0),
 * continuous glitch-free Bezier contour, and spring pop scaling.
 */
class KeyMagnifierPopup {

    private val bubblePath = Path()
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private var activeKey: KeyDefinition? = null
    private var pressStartTime: Long = 0L

    fun onKeyDown(key: KeyDefinition) {
        activeKey = key
        pressStartTime = System.currentTimeMillis()
    }

    fun draw(
        canvas: Canvas,
        key: KeyDefinition,
        theme: ThemeColors,
        density: Float,
        viewWidth: Float
    ) {
        if (key.isModifier() || key.label.length > 2) return

        val bounds = key.bounds
        val bubbleWidth = bounds.width() * 1.38f
        val bubbleHeight = bounds.height() * 1.32f
        val cornerRadius = 10f * density
        val keyCornerRadius = 8f * density
        val minMargin = 4f * density

        // Screen edge clamping: keeps popup within keyboard bounds without clipping
        val idealCenterX = bounds.centerX()
        var bubbleLeft = idealCenterX - (bubbleWidth / 2f)
        var bubbleRight = bubbleLeft + bubbleWidth

        if (bubbleLeft < minMargin) {
            bubbleLeft = minMargin
            bubbleRight = bubbleLeft + bubbleWidth
        } else if (bubbleRight > viewWidth - minMargin) {
            bubbleRight = viewWidth - minMargin
            bubbleLeft = bubbleRight - bubbleWidth
        }

        val bubbleCenterX = (bubbleLeft + bubbleRight) / 2f
        val bubbleTop = bounds.top - bubbleHeight + (4f * density)
        val upperBubbleBottom = bounds.top - (2f * density)

        // Spring pop scaling animation (0.90 -> 1.0 in ~60ms)
        val elapsed = if (activeKey == key) (System.currentTimeMillis() - pressStartTime).coerceIn(0L, 65L) else 65L
        val scale = 0.90f + (0.10f * (elapsed / 65f))

        canvas.save()
        canvas.scale(scale, scale, bounds.centerX(), bounds.centerY())

        // Build continuous, non-self-intersecting smooth balloon path
        bubblePath.reset()

        // 1. Upper bubble top edge & corners
        bubblePath.moveTo(bubbleLeft + cornerRadius, bubbleTop)
        bubblePath.lineTo(bubbleRight - cornerRadius, bubbleTop)
        bubblePath.quadTo(bubbleRight, bubbleTop, bubbleRight, bubbleTop + cornerRadius)

        // 2. Right side of upper bubble down to neck
        bubblePath.lineTo(bubbleRight, upperBubbleBottom)

        // 3. Neck transition from upper bubble right to key right
        val neckCtrlXRight = (bubbleRight + bounds.right) / 2f
        val neckCtrlY = bounds.top + (2f * density)
        bubblePath.quadTo(neckCtrlXRight, neckCtrlY, bounds.right, bounds.top + (6f * density))

        // 4. Down right side of key to bottom-right corner
        bubblePath.lineTo(bounds.right, bounds.bottom - keyCornerRadius)
        bubblePath.quadTo(bounds.right, bounds.bottom, bounds.right - keyCornerRadius, bounds.bottom)

        // 5. Bottom edge of key
        bubblePath.lineTo(bounds.left + keyCornerRadius, bounds.bottom)
        bubblePath.quadTo(bounds.left, bounds.bottom, bounds.left, bounds.bottom - keyCornerRadius)

        // 6. Up left side of key to neck
        bubblePath.lineTo(bounds.left, bounds.top + (6f * density))

        // 7. Neck transition from key left to upper bubble left
        val neckCtrlXLeft = (bubbleLeft + bounds.left) / 2f
        bubblePath.quadTo(neckCtrlXLeft, neckCtrlY, bubbleLeft, upperBubbleBottom)

        // 8. Up left side of upper bubble to top-left corner
        bubblePath.lineTo(bubbleLeft, bubbleTop + cornerRadius)
        bubblePath.quadTo(bubbleLeft, bubbleTop, bubbleLeft + cornerRadius, bubbleTop)
        bubblePath.close()

        // Soft drop shadow for elevated iOS key popup
        shadowPaint.color = theme.popupShadow
        canvas.save()
        canvas.translate(0f, 3f * density)
        canvas.drawPath(bubblePath, shadowPaint)
        canvas.restore()

        // Solid bubble surface
        bubblePaint.color = theme.popupBackground
        canvas.drawPath(bubblePath, bubblePaint)

        // Large magnified character label
        textPaint.color = theme.textPrimary
        textPaint.textSize = bounds.height() * 0.74f
        textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val fontMetrics = textPaint.fontMetrics
        val upperCenterY = (bubbleTop + upperBubbleBottom) / 2f
        val textY = upperCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(key.label, bubbleCenterX, textY, textPaint)

        canvas.restore()
    }
}
