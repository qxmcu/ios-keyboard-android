package org.iosclone.keyboard.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic Apple Intelligence iridescent gradient icon button.
 * Replaces plain emoji with pixel-perfect, centered, hardware-accelerated
 * iridescent Siri / Apple Intelligence glowing emblem.
 */
class AppleIntelligenceIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val iconPath = Path()
    private var isDarkTheme = false
    private var scaleFactor = 1.0f

    private val clickAnimator = ValueAnimator.ofFloat(1.0f, 0.88f, 1.0f).apply {
        duration = 220
        interpolator = OvershootInterpolator(1.5f)
        addUpdateListener {
            scaleFactor = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        isClickable = true
        isFocusable = true
    }

    fun setTheme(isDark: Boolean) {
        if (isDarkTheme != isDark) {
            isDarkTheme = isDark
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scaleFactor = 0.90f
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                clickAnimator.start()
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                scaleFactor = 1.0f
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val cy = h / 2f
        val density = resources.displayMetrics.density

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor, cx, cy)

        val radius = minOf(w, h) / 2f - (1f * density)

        // 1. Frosted Glass Capsule Background
        bgPaint.color = if (isDarkTheme) Color.argb(45, 255, 255, 255) else Color.argb(24, 0, 0, 0)
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // 2. Subtle Glass Border Stroke
        borderPaint.color = if (isDarkTheme) Color.argb(60, 255, 255, 255) else Color.argb(30, 0, 0, 0)
        borderPaint.strokeWidth = 0.75f * density
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // 3. Apple Intelligence Iridescent Shader
        // Apple Intelligence spectrum: Magenta -> Purple -> Indigo -> Cyan -> Amber
        val colors = intArrayOf(
            Color.parseColor("#FF2D55"), // Magenta/Pink
            Color.parseColor("#AF52DE"), // Purple
            Color.parseColor("#5856D6"), // Indigo
            Color.parseColor("#007AFF"), // Electric Blue
            Color.parseColor("#5AC8FA"), // Cyan
            Color.parseColor("#FF9500")  // Warm Amber
        )
        val positions = floatArrayOf(0.0f, 0.22f, 0.44f, 0.66f, 0.85f, 1.0f)

        val iconRadius = radius * 0.58f
        val gradient = LinearGradient(
            cx - iconRadius, cy - iconRadius,
            cx + iconRadius, cy + iconRadius,
            colors, positions,
            Shader.TileMode.CLAMP
        )
        iconPaint.shader = gradient

        // 4. Construct Authentic 4-pointed radiant Apple Intelligence Emblem
        buildEmblemPath(cx, cy, iconRadius)
        canvas.drawPath(iconPath, iconPaint)

        // 5. Center specular highlight / core sparkle
        innerGlowPaint.color = if (isDarkTheme) Color.argb(190, 255, 255, 255) else Color.argb(220, 255, 255, 255)
        canvas.drawCircle(cx, cy, iconRadius * 0.22f, innerGlowPaint)

        // Secondary small satellite sparkle at top right
        val satOffset = iconRadius * 0.65f
        canvas.drawCircle(cx + satOffset, cy - satOffset, iconRadius * 0.12f, innerGlowPaint)

        canvas.restore()
    }

    private fun buildEmblemPath(cx: Float, cy: Float, r: Float) {
        iconPath.reset()
        val innerR = r * 0.28f

        // Draw 4-point radiant star with smooth curved flairs
        for (i in 0..7) {
            val angle = i * Math.PI / 4.0
            val currentR = if (i % 2 == 0) r else innerR
            val x = (cx + currentR * cos(angle)).toFloat()
            val y = (cy + currentR * sin(angle)).toFloat()

            if (i == 0) {
                iconPath.moveTo(x, y)
            } else {
                val prevAngle = (i - 1) * Math.PI / 4.0
                val midAngle = (prevAngle + angle) / 2.0
                val ctrlR = innerR * 0.85f
                val ctrlX = (cx + ctrlR * cos(midAngle)).toFloat()
                val ctrlY = (cy + ctrlR * sin(midAngle)).toFloat()
                iconPath.quadTo(ctrlX, ctrlY, x, y)
            }
        }
        val lastAngle = 7 * Math.PI / 4.0
        val midAngle = (lastAngle + 2 * Math.PI) / 2.0
        val ctrlR = innerR * 0.85f
        val ctrlX = (cx + ctrlR * cos(midAngle)).toFloat()
        val ctrlY = (cy + ctrlR * sin(midAngle)).toFloat()
        iconPath.quadTo(ctrlX, ctrlY, cx + r, cy)
        iconPath.close()
    }
}
