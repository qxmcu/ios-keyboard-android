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
import org.iosclone.keyboard.layout.LanguageLayout
import org.iosclone.keyboard.layout.OneHandedMode
import org.iosclone.keyboard.service.KeyboardActionListener
import org.iosclone.keyboard.settings.KeyboardPreferences
import org.iosclone.keyboard.theme.KeyboardTheme
import org.iosclone.keyboard.theme.ThemeColors
import android.content.Intent
import org.iosclone.keyboard.settings.SettingsActivity
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

    var bottomInset: Float = 0f

    fun setBottomInset(insetPx: Int) {
        val newInset = insetPx.toFloat()
        if (bottomInset != newInset) {
            bottomInset = newInset
            val w = if (width > 0) width.toFloat() else measuredWidth.toFloat()
            val h = if (height > 0) height.toFloat() else measuredHeight.toFloat()
            if (w > 0 && h > 0) {
                layout?.measure(w, h, resources.displayMetrics.density, bottomInset)
            }
            requestLayout()
            invalidate()
        }
    }

    var layout: KeyboardLayout? = null
        set(value) {
            field = value
            val w = if (width > 0) width.toFloat() else measuredWidth.toFloat()
            val h = if (height > 0) height.toFloat() else measuredHeight.toFloat()
            if (w > 0 && h > 0) {
                value?.measure(w, h, resources.displayMetrics.density, bottomInset)
            }
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
    private val keyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val trackpadOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Vector drawable cache
    private val iconCache = mutableMapOf<String, Drawable>()

    // Popups
    var popupOverlay: KeyPopupOverlayView? = null
    private val magnifierPopup = KeyMagnifierPopup()
    private val longPressPopup = LongPressPopup()

    // Glide typing
    private val gestureTrailRenderer = GestureTrailRenderer()
    private val gestureMatcher = GesturePathMatcher()
    private val gestureStrokePoints = mutableListOf<org.iosclone.keyboard.gesture.GesturePoint>()

    // Multi-touch tracking
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var pressedKey: KeyDefinition? = null
    private var isGliding = false
    private var glideStartX = 0f
    private var glideStartY = 0f

    // Continuous Backspace on Hold
    private var isContinuousBackspaceActive = false
    private var backspaceRepeatCount = 0
    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (pressedKey?.keyType == KeyType.DELETE) {
                isContinuousBackspaceActive = true
                backspaceRepeatCount++
                actionListener?.onDelete()
                audioHapticFeedback?.playKeystrokeSound(SoundType.DELETE, preferences.soundEnabled, preferences.soundVolume, this@IOSKeyboardView)
                audioHapticFeedback?.performHapticFeedback(SoundType.DELETE, preferences.hapticsEnabled, preferences.hapticsIntensity, this@IOSKeyboardView)
                val delay = if (backspaceRepeatCount > 15) 35L else 50L
                mainHandler.postDelayed(this, delay)
            }
        }
    }

    // Spacebar Cursor Trackpad Mode
    private var isTrackpadMode = false
    private var trackpadStartX = 0f
    private var trackpadAccumulatedDx = 0f

    // Long press handler
    private var isLongPressActive = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        pressedKey?.let { key ->
            if (key.keyType == KeyType.SPACE && preferences.spacebarTrackpadEnabled) {
                // Activate Trackpad Mode!
                isTrackpadMode = true
                popupOverlay?.hideMagnifier()
                audioHapticFeedback?.performLongPressHaptic(preferences.hapticsEnabled, preferences.hapticsIntensity, this)
                invalidate()
            } else if (key.keyType == KeyType.GLOBE || key.keyType == KeyType.EMOJI) {
                audioHapticFeedback?.performLongPressHaptic(preferences.hapticsEnabled, preferences.hapticsIntensity, this)
                showLanguageContextMenu()
            } else if (key.accents.isNotEmpty()) {
                isLongPressActive = true
                popupOverlay?.showLongPress(key, this)
                audioHapticFeedback?.performLongPressHaptic(preferences.hapticsEnabled, preferences.hapticsIntensity, this)
                invalidate()
            }
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isHapticFeedbackEnabled = true
        isSoundEffectsEnabled = true
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        loadIcons()
    }

    private fun loadIcons() {
        val names = listOf(
            "ic_shift", "ic_shift_caps", "ic_backspace", "ic_globe",
            "ic_dictation", "ic_emoji", "ic_return", "ic_onehand_left", "ic_onehand_right"
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
        val screenHeightDp = resources.configuration.screenHeightDp.toFloat()
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Adaptive row height: ergonomically scaled to device screen height (Samsung, Nothing, Pixel)
        val targetRowHeight = when {
            isLandscape -> (32f * density)
            screenHeightDp > 850f -> (46f * density)  // Tall phones / Samsung Galaxy Ultra / Plus
            screenHeightDp > 740f -> (43.5f * density) // Standard devices (Nothing Phone, Galaxy S, Pixel)
            else -> (41f * density)                  // Compact screens
        }

        val rowCount = layout?.rows?.size ?: (if (preferences.showNumberRow) 5 else 4)
        val verticalRowGap = if (isLandscape) (6f * density) else (10f * density)
        val topPadding = 6f * density
        val floatingBarHeight = if (isLandscape) 0f else (42f * density)
        val totalKeysHeight = (targetRowHeight * rowCount) + (verticalRowGap * (rowCount - 1)) + topPadding + (6f * density)

        val userBottomSpacing = (preferences.bottomSpacingDp * density).toInt()
        val effectiveBottomInset = bottomInset + userBottomSpacing
        val baseHeight = (totalKeysHeight * preferences.keyboardHeightFactor) + floatingBarHeight + effectiveBottomInset
        val height = baseHeight.toInt()
        setMeasuredDimension(width, height)

        layout?.measure(width.toFloat(), height.toFloat(), density, effectiveBottomInset)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val density = resources.displayMetrics.density
        val userBottomSpacing = (preferences.bottomSpacingDp * density).toInt()
        val effectiveBottomInset = bottomInset + userBottomSpacing
        layout?.measure(w.toFloat(), h.toFloat(), density, effectiveBottomInset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val l = layout ?: return
        val density = resources.displayMetrics.density

        // 1. Keys Rendering (Root container renders translucent blur backdrop)
        val keyCornerRadius = 8.5f * density
        val shadowOffset = 1.35f * density

        for (row in l.rows) {
            for (key in row) {
                val isPressed = (key == pressedKey && !isTrackpadMode)
                drawKey(canvas, key, isPressed, keyCornerRadius, shadowOffset, density)
            }
        }

        // 2. Floating bottom bar buttons (Globe on left, Dictation on right)
        val bottomIconTint = if (currentTheme.isDark) android.graphics.Color.parseColor("#AEAEB2") else android.graphics.Color.parseColor("#48484A")
        if (!l.globeButtonBounds.isEmpty) {
            val pressedGlobe = (pressedKey == l.globeKeyDefinition)
            val globeTint = if (pressedGlobe) currentTheme.accentBlue else bottomIconTint
            drawKeyIcon(canvas, "ic_globe", l.globeButtonBounds, globeTint, density, iconSizeDp = 22.5f)
        }
        if (!l.dictationButtonBounds.isEmpty) {
            val pressedDict = (pressedKey == l.dictationKeyDefinition)
            val dictTint = if (pressedDict) currentTheme.accentBlue else bottomIconTint
            drawKeyIcon(canvas, "ic_dictation", l.dictationButtonBounds, dictTint, density, iconSizeDp = 22.5f)
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

        // 6. Long-press accent popover & Key Magnifier Popup Bubble (when no top-level overlay attached)
        if (popupOverlay == null) {
            val currentPressed = pressedKey
            if (currentPressed != null && isLongPressActive && currentPressed.accents.isNotEmpty() && !isGliding && !isTrackpadMode) {
                longPressPopup.draw(canvas, currentPressed, width.toFloat(), currentTheme, density)
            } else if (currentPressed != null && preferences.keyPopupsEnabled && !isGliding && !isTrackpadMode && !isLongPressActive) {
                magnifierPopup.draw(canvas, currentPressed, currentTheme, density, width.toFloat())
            }
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
        val retLabel = layout?.returnKeyLabel?.lowercase() ?: "return"
        val isSpecialBlueReturn = (key.keyType == KeyType.RETURN && (retLabel == "search" || retLabel == "go" || retLabel == "send"))
        val isShiftActive = key.keyType == KeyType.SHIFT && (layout?.mode == KeyboardMode.UPPERCASE || layout?.mode == KeyboardMode.CAPS_LOCK)

        canvas.save()
        if (isPressed && !isTrackpadMode) {
            canvas.scale(0.965f, 0.965f, bounds.centerX(), bounds.centerY())
        }

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
            isShiftActive -> currentTheme.modifierKeyBackgroundPressed
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
                val tint = if (isShiftActive && currentTheme.isDark) 0xFF000000.toInt() else currentTheme.textPrimary
                drawKeyIcon(canvas, iconName, bounds, tint, density)
            }
            KeyType.DELETE -> {
                drawKeyIcon(canvas, "ic_backspace", bounds, currentTheme.textPrimary, density)
            }
            KeyType.GLOBE -> {
                drawKeyIcon(canvas, "ic_globe", bounds, currentTheme.textPrimary, density)
            }
            KeyType.EMOJI -> {
                drawKeyIcon(canvas, "ic_emoji", bounds, currentTheme.textPrimary, density)
            }
            KeyType.DICTATION -> {
                drawKeyIcon(canvas, "ic_dictation", bounds, currentTheme.textPrimary, density)
            }
            KeyType.RETURN -> {
                val displayLabel = layout?.returnKeyLabel ?: "return"
                textPaint.color = if (isSpecialBlueReturn) currentTheme.returnKeyText else currentTheme.textPrimary
                if (isSpecialBlueReturn || (displayLabel.lowercase() != "return" && displayLabel != "↵")) {
                    textPaint.textSize = (if (displayLabel.length > 4) 14f else 15.5f) * density
                    textPaint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    drawCenteredText(canvas, displayLabel, bounds, textPaint)
                } else {
                    drawKeyIcon(canvas, "ic_return", bounds, currentTheme.textPrimary, density, iconSizeDp = 20f)
                }
            }
            KeyType.SPACE -> {
                // Spacebar has clean surface (no center text) matching iOS 27 Liquid Glass
                if (!isTrackpadMode) {
                    val badge = when (layout?.language) {
                        LanguageLayout.QWERTY -> "EN"
                        LanguageLayout.SPANISH -> "ES"
                        LanguageLayout.AZERTY -> "FR"
                        LanguageLayout.QWERTZ -> "DE"
                        LanguageLayout.CYRILLIC -> "RU"
                        LanguageLayout.ARABIC -> "AR"
                        LanguageLayout.HINDI -> "HI"
                        LanguageLayout.CHINESE_PINYIN -> "拼"
                        LanguageLayout.JAPANESE -> "日"
                        LanguageLayout.KOREAN -> "한"
                        LanguageLayout.THAI -> "ไทย"
                        else -> "EN"
                    }
                    secondaryTextPaint.color = currentTheme.textSecondary
                    secondaryTextPaint.textSize = 9.5f * density
                    secondaryTextPaint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    canvas.drawText(badge, bounds.right - (12f * density), bounds.bottom - (5.5f * density), secondaryTextPaint)
                }
            }
            KeyType.SWITCH_NUMERIC, KeyType.SWITCH_ALPHA, KeyType.SWITCH_SYMBOL -> {
                textPaint.color = currentTheme.textPrimary
                textPaint.textSize = 15f * density
                textPaint.typeface = android.graphics.Typeface.DEFAULT
                drawCenteredText(canvas, key.label, bounds, textPaint)
            }
            else -> {
                textPaint.color = currentTheme.textPrimary
                textPaint.textSize = (if (key.label.length > 1) 16f else 23f) * density
                textPaint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                drawCenteredText(canvas, key.label, bounds, textPaint)
            }
        }
        canvas.restore()
    }

    private fun drawCenteredText(canvas: Canvas, text: String, bounds: RectF, paint: Paint) {
        val fm = paint.fontMetrics
        val y = bounds.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, bounds.centerX(), y, paint)
    }

    private fun drawKeyIcon(canvas: Canvas, iconName: String, bounds: RectF, tintColor: Int, density: Float, iconSizeDp: Float = 20f) {
        val drawable = iconCache[iconName] ?: return
        val iconSize = (iconSizeDp * density).toInt()
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
                isLongPressActive = false
                longPressPopup.dismiss()
                gestureTrailRenderer.clear()
                gestureStrokePoints.clear()
                gestureStrokePoints.add(org.iosclone.keyboard.gesture.GesturePoint(x, y, System.currentTimeMillis()))

                mainHandler.removeCallbacks(backspaceRepeatRunnable)
                isContinuousBackspaceActive = false
                backspaceRepeatCount = 0

                // Check one-handed docking toggle
                if (l.oneHandedMode != OneHandedMode.NORMAL && l.oneHandedSideButtonBounds.contains(x, y)) {
                    l.oneHandedMode = if (l.oneHandedMode == OneHandedMode.LEFT_DOCKED) OneHandedMode.RIGHT_DOCKED else OneHandedMode.LEFT_DOCKED
                    l.measure(width.toFloat(), height.toFloat(), resources.displayMetrics.density, bottomInset)
                    invalidate()
                    return true
                }

                val key = l.findKeyAt(x, y)
                pressedKey = key
                if (key != null) {
                    playKeyFeedback(key)
                    if (key.keyType == KeyType.DELETE) {
                        popupOverlay?.hideMagnifier()
                        actionListener?.onDelete()
                        mainHandler.postDelayed(backspaceRepeatRunnable, 400L)
                    } else {
                        if (preferences.keyPopupsEnabled) {
                            popupOverlay?.showMagnifier(key, this) ?: magnifierPopup.onKeyDown(key)
                        }
                        mainHandler.postDelayed(longPressRunnable, 450L)
                    }
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

                if (isLongPressActive) {
                    popupOverlay?.updateLongPressSelection(x) ?: longPressPopup.updateSelection(x)
                    invalidate()
                    return true
                }

                if (isGliding) {
                    popupOverlay?.hideMagnifier()
                    gestureStrokePoints.add(org.iosclone.keyboard.gesture.GesturePoint(x, y, System.currentTimeMillis()))
                    gestureTrailRenderer.addPoint(x, y)
                    invalidate()
                    return true
                }

                // Check glide gesture initiation
                val dist = abs(x - glideStartX) + abs(y - glideStartY)
                val density = resources.displayMetrics.density
                if (dist > (16f * density) && preferences.gestureTypingEnabled && pressedKey?.keyType == KeyType.CHARACTER) {
                    isGliding = true
                    popupOverlay?.hideMagnifier()
                    mainHandler.removeCallbacks(longPressRunnable)
                    gestureTrailRenderer.addPoint(glideStartX, glideStartY)
                    gestureTrailRenderer.addPoint(x, y)
                    gestureStrokePoints.add(org.iosclone.keyboard.gesture.GesturePoint(x, y, System.currentTimeMillis()))
                    invalidate()
                    return true
                }

                if (pressedKey?.keyType == KeyType.DELETE) {
                    val curKey = l.findKeyAt(x, y)
                    if (curKey != pressedKey) {
                        mainHandler.removeCallbacks(backspaceRepeatRunnable)
                        isContinuousBackspaceActive = false
                    }
                }

                // Normal key sliding
                val key = l.findKeyAt(x, y)
                if (key != pressedKey && !isGliding) {
                    mainHandler.removeCallbacks(longPressRunnable)
                    mainHandler.removeCallbacks(backspaceRepeatRunnable)
                    pressedKey = key
                    isLongPressActive = false
                    popupOverlay?.hideLongPress() ?: longPressPopup.dismiss()
                    if (key != null) {
                        if (key.keyType == KeyType.DELETE) {
                            popupOverlay?.hideMagnifier()
                            isContinuousBackspaceActive = false
                            backspaceRepeatCount = 0
                            actionListener?.onDelete()
                            mainHandler.postDelayed(backspaceRepeatRunnable, 400L)
                        } else {
                            if (preferences.keyPopupsEnabled) {
                                popupOverlay?.showMagnifier(key, this) ?: magnifierPopup.onKeyDown(key)
                            }
                            mainHandler.postDelayed(longPressRunnable, 450L)
                        }
                    } else {
                        popupOverlay?.hideMagnifier()
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.removeCallbacks(backspaceRepeatRunnable)
                popupOverlay?.hideMagnifier()

                if (isTrackpadMode) {
                    isTrackpadMode = false
                    isLongPressActive = false
                    pressedKey = null
                    invalidate()
                    return true
                }

                if (isLongPressActive) {
                    val key = pressedKey
                    if (key != null) {
                        val selectedAccent = popupOverlay?.getSelectedAccent(key) ?: longPressPopup.getSelectedCharacter(key)
                        if (selectedAccent != null) {
                            actionListener?.onText(selectedAccent)
                        } else {
                            dispatchKeyAction(key)
                        }
                    }
                    popupOverlay?.hideLongPress() ?: longPressPopup.dismiss()
                    isLongPressActive = false
                    pressedKey = null
                    invalidate()
                    return true
                }

                popupOverlay?.hideLongPress() ?: longPressPopup.dismiss()

                if (isGliding) {
                    isGliding = false
                    val candidates = gestureMatcher.match(gestureStrokePoints, l, actionListener?.getDictionaryTrie())
                    if (candidates.isNotEmpty()) {
                        actionListener?.onGlideTypingCompleted(candidates)
                    }
                    gestureStrokePoints.clear()
                    gestureTrailRenderer.clear()
                    pressedKey = null
                    invalidate()
                    return true
                }

                val key = pressedKey
                if (key != null) {
                    if (key.keyType == KeyType.DELETE) {
                        isContinuousBackspaceActive = false
                    } else {
                        dispatchKeyAction(key)
                    }
                }

                pressedKey = null
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.removeCallbacks(backspaceRepeatRunnable)
                isContinuousBackspaceActive = false
                gestureStrokePoints.clear()
                gestureTrailRenderer.clear()
                isLongPressActive = false
                popupOverlay?.hideMagnifier()
                popupOverlay?.hideLongPress() ?: longPressPopup.dismiss()
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
        audioHapticFeedback?.playKeystrokeSound(soundType, preferences.soundEnabled, preferences.soundVolume, this)
        audioHapticFeedback?.performHapticFeedback(soundType, preferences.hapticsEnabled, preferences.hapticsIntensity, this)
    }

    fun showLanguageContextMenu() {
        val l = layout ?: return
        val density = resources.displayMetrics.density
        val anchorX = if (!l.globeButtonBounds.isEmpty) l.globeButtonBounds.centerX() else (30f * density)
        val anchorY = if (!l.globeButtonBounds.isEmpty) l.globeButtonBounds.top else (height - (50f * density))
        IOSContextMenu(
            context = context,
            currentTheme = currentTheme,
            activeLanguage = l.language,
            currentOneHandedMode = l.oneHandedMode,
            onLanguageSelected = { lang -> actionListener?.onLanguageSelected(lang) },
            onEmojiSelected = { actionListener?.onEmojiPickerRequested() },
            onSettingsSelected = {
                try {
                    val intent = Intent(context, SettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            },
            onOneHandedSelected = { mode -> actionListener?.onOneHandedModeChange(mode) }
        ).show(this, anchorX, anchorY)
    }
}
