package org.iosclone.keyboard.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import org.iosclone.keyboard.audio.AudioHapticFeedback
import org.iosclone.keyboard.audio.SoundType
import org.iosclone.keyboard.gesture.GesturePathMatcher
import org.iosclone.keyboard.gesture.GestureTrailRenderer
import org.iosclone.keyboard.layout.KeyDefinition
import org.iosclone.keyboard.layout.KeyType
import org.iosclone.keyboard.layout.KeyboardLayout
import org.iosclone.keyboard.layout.KeyboardMode
import org.iosclone.keyboard.layout.OneHandedMode
import org.iosclone.keyboard.service.KeyboardActionListener
import org.iosclone.keyboard.settings.KeyboardPreferences
import org.iosclone.keyboard.theme.KeyboardTheme
import org.iosclone.keyboard.theme.ThemeColors
import kotlin.math.abs

class IOSKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var actionListener: KeyboardActionListener? = null
    var audioHapticFeedback: AudioHapticFeedback? = null
    var preferences: KeyboardPreferences = KeyboardPreferences(context)

    private val themeResolver = KeyboardTheme(context)
    var currentTheme: ThemeColors = themeResolver.resolveTheme(preferences.themeMode)
        private set

    var layout: KeyboardLayout? = null
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    // Drawing paints
    private val keyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val trackpadOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Vector drawable cache
    private val iconCache = mutableMapOf<String, Drawable>()

    // Popups
    private val magnifierPopup = KeyMagnifierPopup()
    private val longPressPopup = LongPressPopup()

    // Glide typing
    private val gestureTrailRenderer = GestureTrailRenderer()
    private val gestureMatcher = GesturePathMatcher()

    // Multi-touch tracking
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var pressedKey: KeyDefinition? = null
    private var isGliding = false
    private var glideStartX = 0f
    private var glideStartY = 0f

    // Spacebar Cursor Trackpad Mode
    private var isTrackpadMode = false
    private var trackpadStartX = 0f
    private var trackpadAccumulatedDx = 0f

    // Long press handler
    private val mainHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        pressedKey?.let { key ->
            if (key.keyType == KeyType.SPACE && preferences.spacebarTrackpadEnabled) {
                // Activate Trackpad Mode!
                isTrackpadMode = true
                audioHapticFeedback?.performLongPressHaptic(preferences.hapticsEnabled, preferences.hapticsIntensity)
                invalidate()
            } else if (key.accents.isNotEmpty()) {
                audioHapticFeedback?.performLongPressHaptic(preferences.hapticsEnabled, preferences.hapticsIntensity)
                invalidate()
            }
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        loadIcons()
    }

    private fun loadIcons() {
        val names = listOf(
            "ic_shift", "ic_shift_caps", "ic_backspace", "ic_globe",
            "ic_dictation", "ic_emoji", "ic_onehand_left", "ic_onehand_right"
        )
        for (name in names) {
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) {
                ContextCompat.getDrawable(context, id)?.let { iconCache[name] = it }
            }
        }
    }

    fun applyTheme(theme: ThemeColors) {
        currentTheme = theme
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val baseHeight = (220f * density * preferences.keyboardHeightFactor).toInt()
        val height = resolveSize(baseHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val density = resources.displayMetrics.density
        layout?.measure(w.toFloat(), h.toFloat(), density)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val l = layout ?: return
        val density = resources.displayMetrics.density

        // 1. Keyboard Canvas Background
        canvas.drawColor(currentTheme.keyboardBackground)

        // 2. Keys Rendering
        val keyCornerRadius = 5f * density
        val shadowOffset = 1.2f * density

        for (row in l.rows) {
            for (key in row) {
                val isPressed = (key == pressedKey && !isTrackpadMode)
                drawKey(canvas, key, isPressed, keyCornerRadius, shadowOffset, density)
            }
        }

        // 3. One-handed docking sidebar button
        if (l.oneHandedMode != OneHandedMode.NORMAL && !l.oneHandedSideButtonBounds.isEmpty) {
            drawOneHandedSidebar(canvas, l, density)
        }

        // 4. Trackpad Mode Overlay
        if (isTrackpadMode) {
            trackpadOverlayPaint.color = currentTheme.trackpadHighlightColor
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), trackpadOverlayPaint)
        }

        // 5. Glide Typing Trail
        if (isGliding && preferences.gestureTypingEnabled) {
            gestureTrailRenderer.draw(canvas, currentTheme.gestureTrailColor, 8f * density)
        }

        // 6. Long-press accent popover
        val currentPressed = pressedKey
        if (currentPressed != null && currentPressed.accents.isNotEmpty() && !isGliding && !isTrackpadMode) {
            longPressPopup.draw(canvas, currentPressed, width.toFloat(), currentTheme, density)
        } else if (currentPressed != null && preferences.keyPopupsEnabled && !isGliding && !isTrackpadMode) {
            // 7. Key Magnifier Popup Bubble
            magnifierPopup.draw(canvas, currentPressed, currentTheme, density)
        }
    }

    private fun drawKey(
        canvas: Canvas,
        key: KeyDefinition,
        isPressed: Boolean,
        cornerRadius: Float,
        shadowOffset: Float,
        density: Float
    ) {
        val bounds = key.bounds
        val isModifier = key.isModifier()
        val isSpecialBlueReturn = (key.keyType == KeyType.RETURN && layout?.returnKeyLabel?.lowercase() != "return")

        // Dim keys during trackpad mode
        val alphaMultiplier = if (isTrackpadMode) 0.4f else 1.0f

        // Draw physical bottom drop shadow
        keyShadowPaint.color = currentTheme.keyShadow
        keyShadowPaint.alpha = (255 * alphaMultiplier).toInt()
        val shadowRect = RectF(bounds.left, bounds.top + shadowOffset, bounds.right, bounds.bottom + shadowOffset)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, keyShadowPaint)

        // Key surface background
        val surfaceColor = when {
            isSpecialBlueReturn -> currentTheme.returnKeyBlue
            isModifier -> if (isPressed) currentTheme.modifierKeyBackgroundPressed else currentTheme.modifierKeyBackground
            else -> if (isPressed) currentTheme.keyBackgroundPressed else currentTheme.keyBackground
        }
        keyBgPaint.color = surfaceColor
        keyBgPaint.alpha = (255 * alphaMultiplier).toInt()
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, keyBgPaint)

        // Key Content (Icon or Label)
        when (key.keyType) {
            KeyType.SHIFT -> {
                val iconName = if (layout?.mode == KeyboardMode.CAPS_LOCK) "ic_shift_caps" else "ic_shift"
                drawKeyIcon(canvas, iconName, bounds, currentTheme.textPrimary, density)
            }
            KeyType.DELETE -> {
                drawKeyIcon(canvas, "ic_backspace", bounds, currentTheme.textPrimary, density)
            }
            KeyType.GLOBE -> {
                drawKeyIcon(canvas, "ic_globe", bounds, currentTheme.textPrimary, density)
            }
            KeyType.DICTATION -> {
                drawKeyIcon(canvas, "ic_dictation", bounds, currentTheme.textPrimary, density)
            }
            KeyType.RETURN -> {
                val label = layout?.returnKeyLabel ?: "return"
                textPaint.color = if (isSpecialBlueReturn) currentTheme.returnKeyText else currentTheme.textPrimary
                textPaint.textSize = (if (label.length > 3) 15f else 16f) * density
                drawCenteredText(canvas, label, bounds, textPaint)
            }
            KeyType.SPACE -> {
                textPaint.color = currentTheme.textSecondary
                textPaint.textSize = 14f * density
                val label = if (isTrackpadMode) "" else (layout?.language?.displayName ?: "space")
                drawCenteredText(canvas, label, bounds, textPaint)
            }
            else -> {
                textPaint.color = currentTheme.textPrimary
                textPaint.textSize = (if (key.label.length > 2) 14f else 22f) * density
                drawCenteredText(canvas, key.label, bounds, textPaint)
            }
        }
    }

    private fun drawCenteredText(canvas: Canvas, text: String, bounds: RectF, paint: Paint) {
        val fm = paint.fontMetrics
        val y = bounds.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, bounds.centerX(), y, paint)
    }

    private fun drawKeyIcon(canvas: Canvas, iconName: String, bounds: RectF, tintColor: Int, density: Float) {
        val drawable = iconCache[iconName] ?: return
        val iconSize = (20f * density).toInt()
        val left = (bounds.centerX() - iconSize / 2f).toInt()
        val top = (bounds.centerY() - iconSize / 2f).toInt()
        drawable.setBounds(left, top, left + iconSize, top + iconSize)
        drawable.setTint(tintColor)
        drawable.draw(canvas)
    }

    private fun drawOneHandedSidebar(canvas: Canvas, l: KeyboardLayout, density: Float) {
        val b = l.oneHandedSideButtonBounds
        val iconName = if (l.oneHandedMode == OneHandedMode.LEFT_DOCKED) "ic_onehand_right" else "ic_onehand_left"
        val drawable = iconCache[iconName] ?: return
        val iconSize = (28f * density).toInt()
        val left = (b.centerX() - iconSize / 2f).toInt()
        val top = (b.centerY() - iconSize / 2f).toInt()
        drawable.setBounds(left, top, left + iconSize, top + iconSize)
        drawable.setTint(currentTheme.accentBlue)
        drawable.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val l = layout ?: return false
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                glideStartX = x
                glideStartY = y
                isGliding = false
                isTrackpadMode = false
                gestureTrailRenderer.clear()

                // Check one-handed docking toggle
                if (l.oneHandedMode != OneHandedMode.NORMAL && l.oneHandedSideButtonBounds.contains(x, y)) {
                    l.oneHandedMode = if (l.oneHandedMode == OneHandedMode.LEFT_DOCKED) OneHandedMode.RIGHT_DOCKED else OneHandedMode.LEFT_DOCKED
                    l.measure(width.toFloat(), height.toFloat(), resources.displayMetrics.density)
                    invalidate()
                    return true
                }

                val key = l.findKeyAt(x, y)
                pressedKey = key
                if (key != null) {
                    playKeyFeedback(key)
                    mainHandler.postDelayed(longPressRunnable, 350)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTrackpadMode) {
                    val deltaX = x - trackpadStartX
                    trackpadAccumulatedDx += deltaX
                    trackpadStartX = x
                    val density = resources.displayMetrics.density
                    val threshold = 12f * density
                    if (abs(trackpadAccumulatedDx) >= threshold) {
                        val steps = (trackpadAccumulatedDx / threshold).toInt()
                        actionListener?.onCursorMoved(steps)
                        trackpadAccumulatedDx -= (steps * threshold)
                    }
                    return true
                }

                if (longPressPopup.isShowing()) {
                    longPressPopup.updateSelection(x)
                    invalidate()
                    return true
                }

                // Check glide gesture initiation
                val dist = abs(x - glideStartX) + abs(y - glideStartY)
                val density = resources.displayMetrics.density
                if (dist > (16f * density) && preferences.gestureTypingEnabled && pressedKey?.keyType == KeyType.CHARACTER) {
                    if (!isGliding) {
                        isGliding = true
                        mainHandler.removeCallbacks(longPressRunnable)
                        gestureTrailRenderer.addPoint(glideStartX, glideStartY)
                    }
                    gestureTrailRenderer.addPoint(x, y)
                    invalidate()
                    return true
                }

                // Normal key sliding
                val key = l.findKeyAt(x, y)
                if (key != pressedKey && !isGliding) {
                    pressedKey = key
                    mainHandler.removeCallbacks(longPressRunnable)
                    if (key != null) {
                        mainHandler.postDelayed(longPressRunnable, 350)
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(longPressRunnable)

                if (isTrackpadMode) {
                    isTrackpadMode = false
                    pressedKey = null
                    invalidate()
                    return true
                }

                if (longPressPopup.isShowing()) {
                    val key = pressedKey
                    if (key != null) {
                        val selectedAccent = longPressPopup.getSelectedCharacter(key)
                        if (selectedAccent != null) {
                            actionListener?.onText(selectedAccent)
                        }
                    }
                    longPressPopup.dismiss()
                    pressedKey = null
                    invalidate()
                    return true
                }

                if (isGliding) {
                    isGliding = false
                    val points = gestureTrailRenderer.getPoints()
                    val candidates = gestureMatcher.match(points, l, actionListener?.getDictionaryTrie())
                    if (candidates.isNotEmpty()) {
                        actionListener?.onGlideTypingCompleted(candidates)
                    }
                    gestureTrailRenderer.clear()
                    pressedKey = null
                    invalidate()
                    return true
                }

                val key = pressedKey
                if (key != null) {
                    dispatchKeyAction(key)
                }

                pressedKey = null
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                longPressPopup.dismiss()
                gestureTrailRenderer.clear()
                isGliding = false
                isTrackpadMode = false
                pressedKey = null
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun dispatchKeyAction(key: KeyDefinition) {
        when (key.keyType) {
            KeyType.CHARACTER -> actionListener?.onText(key.label)
            KeyType.SPACE -> actionListener?.onKey(key)
            KeyType.DELETE -> actionListener?.onDelete()
            KeyType.SHIFT -> actionListener?.onShiftToggle()
            KeyType.RETURN -> actionListener?.onKey(key)
            KeyType.SWITCH_NUMERIC -> actionListener?.onModeChange(KeyboardMode.NUMERIC)
            KeyType.SWITCH_ALPHA -> actionListener?.onModeChange(KeyboardMode.LOWERCASE)
            KeyType.SWITCH_SYMBOL -> actionListener?.onModeChange(KeyboardMode.SYMBOL)
            KeyType.GLOBE -> actionListener?.onLanguageSwitch()
            KeyType.EMOJI -> actionListener?.onEmojiPickerRequested()
            KeyType.DICTATION -> actionListener?.onKey(key)
            else -> actionListener?.onKey(key)
        }
    }

    private fun playKeyFeedback(key: KeyDefinition) {
        val soundType = when (key.keyType) {
            KeyType.DELETE -> SoundType.DELETE
            KeyType.RETURN, KeyType.SPACE -> SoundType.RETURN_SPACE
            else -> SoundType.STANDARD
        }
        audioHapticFeedback?.playKeystrokeSound(soundType, preferences.soundEnabled, preferences.soundVolume)
        audioHapticFeedback?.performHapticFeedback(soundType, preferences.hapticsEnabled, preferences.hapticsIntensity)
    }
}
