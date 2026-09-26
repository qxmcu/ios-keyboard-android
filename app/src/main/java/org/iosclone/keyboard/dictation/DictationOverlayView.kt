package org.iosclone.keyboard.dictation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.iosclone.keyboard.theme.ThemeColors

/**
 * Modern iOS voice dictation overlay featuring an animated Siri-style audio wave visualizer.
 */
class DictationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusTv: TextView
    private val waveView: AudioWaveformView
    private val doneBtn: TextView
    private var currentTheme: ThemeColors = ThemeColors.Light

    var onDoneClicked: (() -> Unit)? = null
    var onRetryClicked: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val density = resources.displayMetrics.density
        val padH = (16 * density).toInt()
        val padV = (8 * density).toInt()
        setPadding(padH, padV, padH, padV)

        statusTv = TextView(context).apply {
            text = "🎙 Listening…"
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (12 * density).toInt()
            }
            setOnClickListener { onRetryClicked?.invoke() }
        }
        addView(statusTv)

        waveView = AudioWaveformView(context).apply {
            layoutParams = LayoutParams(0, (28 * density).toInt(), 1.0f)
        }
        addView(waveView)

        doneBtn = TextView(context).apply {
            text = "Done"
            textSize = 15.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val btnPadH = (14 * density).toInt()
            val btnPadV = (6 * density).toInt()
            setPadding(btnPadH, btnPadV, btnPadH, btnPadV)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = (12 * density).toInt()
            }
            setOnClickListener { onDoneClicked?.invoke() }
        }
        addView(doneBtn)
    }

    fun setStatus(text: String) {
        statusTv.text = text
    }

    fun setAudioLevel(rmsDb: Float) {
        waveView.updateRms(rmsDb)
    }

    fun applyTheme(theme: ThemeColors) {
        currentTheme = theme
        val density = resources.displayMetrics.density
        background = GradientDrawable().apply {
            setColor(theme.keyboardBackground)
            cornerRadius = 14f * density
            setStroke((1 * density).toInt().coerceAtLeast(1), theme.keyboardGlassStroke)
        }
        statusTv.setTextColor(theme.textPrimary)
        waveView.setWaveColor(theme.accentBlue)
        doneBtn.setTextColor(Color.WHITE)
        doneBtn.background = GradientDrawable().apply {
            setColor(theme.accentBlue)
            cornerRadius = 14f * density
        }
    }

    private class AudioWaveformView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 3f * resources.displayMetrics.density
        }

        private var rms: Float = 0f
        private val bars = FloatArray(16) { 0.2f }

        fun updateRms(level: Float) {
            this.rms = (level.coerceIn(-2f, 10f) + 2f) / 12f
            for (i in bars.indices) {
                // Generate natural audio wave variance
                val factor = (Math.sin(System.currentTimeMillis() * 0.01 + i) + 1.0) / 2.0
                bars[i] = (0.15f + (rms * factor.toFloat() * 0.85f)).coerceIn(0.1f, 1.0f)
            }
            invalidate()
        }

        fun setWaveColor(color: Int) {
            paint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val centerY = h / 2f
            val spacing = w / (bars.size + 1)

            for (i in bars.indices) {
                val x = spacing * (i + 1)
                val barH = (h * 0.85f) * bars[i]
                val top = centerY - barH / 2f
                val bottom = centerY + barH / 2f
                canvas.drawLine(x, top, x, bottom, paint)
            }
        }
    }
}
