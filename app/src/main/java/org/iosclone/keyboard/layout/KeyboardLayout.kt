package org.iosclone.keyboard.layout

import android.graphics.RectF

enum class OneHandedMode {
    NORMAL,
    LEFT_DOCKED,
    RIGHT_DOCKED
}

class KeyboardLayout(
    val rows: List<List<KeyDefinition>>,
    val mode: KeyboardMode,
    val language: LanguageLayout
) {
    var oneHandedMode: OneHandedMode = OneHandedMode.NORMAL
    var returnKeyLabel: String = "return"

    // Action button bounds for one-handed docking sidebar
    val oneHandedSideButtonBounds: RectF = RectF()

    // Floating bottom inset buttons (Globe & Dictation)
    val globeButtonBounds: RectF = RectF()
    val dictationButtonBounds: RectF = RectF()
    val globeKeyDefinition = KeyDefinition(code = -10, label = "🌐", keyType = KeyType.GLOBE, weight = 1.0f)
    val dictationKeyDefinition = KeyDefinition(code = -11, label = "🎙", keyType = KeyType.DICTATION, weight = 1.0f)

    /**
     * Dynamically calculates bounds and touch targets for each key across all rows
     * with authentic iOS proportions, automatically adapting across all Android phone models
     * (Nothing Phone, Samsung Galaxy S/A/Ultra/Fold, Google Pixel, etc.).
     */
    fun measure(
        viewWidth: Float,
        viewHeight: Float,
        density: Float,
        bottomInset: Float = 0f
    ) {
        if (rows.isEmpty() || viewWidth <= 0 || viewHeight <= 0) return

        val widthDp = viewWidth / density

        // Adaptive gap & margin scaling based on screen width
        val (horizontalKeyGap, outerHorizontalMargin) = when {
            widthDp < 350f -> Pair(4.0f * density, 3.0f * density)  // Compact / Fold cover screen
            widthDp < 400f -> Pair(5.5f * density, 4.0f * density)  // Standard phone (Nothing, Galaxy S24)
            widthDp < 500f -> Pair(6.5f * density, 6.0f * density)  // Wide phone (Galaxy Ultra, Pro Max)
            else -> Pair(8.0f * density, 12.0f * density)           // Tablet / Fold inner screen
        }

        val topPadding = 6f * density
        val verticalRowGap = 10f * density

        // Dedicated floating bottom bar height (sits cleanly ABOVE system navigation bar)
        val floatingBarHeight = 42f * density
        val bottomPadding = bottomInset + floatingBarHeight + (6f * density)

        val numRows = rows.size
        val availableKeysHeight = viewHeight - topPadding - bottomPadding - (verticalRowGap * (numRows - 1))
        val rowHeight = (availableKeysHeight / numRows).coerceIn(36f * density, 52f * density)

        // Tablet / Foldable centering clamp (prevents awkward ultra-wide stretching)
        val maxKeyboardWidth = if (widthDp > 520f) (480f * density) else viewWidth
        val baseCenterOffset = (viewWidth - maxKeyboardWidth) / 2f

        // One-handed geometry calculation
        val oneHandedFactor = 0.82f
        val effectiveWidth: Float
        val startXOffset: Float

        when (oneHandedMode) {
            OneHandedMode.NORMAL -> {
                effectiveWidth = maxKeyboardWidth
                startXOffset = baseCenterOffset
                oneHandedSideButtonBounds.setEmpty()
            }
            OneHandedMode.LEFT_DOCKED -> {
                effectiveWidth = maxKeyboardWidth * oneHandedFactor
                startXOffset = baseCenterOffset
                oneHandedSideButtonBounds.set(
                    baseCenterOffset + effectiveWidth,
                    topPadding,
                    baseCenterOffset + maxKeyboardWidth,
                    viewHeight - bottomPadding
                )
            }
            OneHandedMode.RIGHT_DOCKED -> {
                effectiveWidth = maxKeyboardWidth * oneHandedFactor
                startXOffset = baseCenterOffset + (maxKeyboardWidth - effectiveWidth)
                oneHandedSideButtonBounds.set(
                    baseCenterOffset,
                    topPadding,
                    startXOffset,
                    viewHeight - bottomPadding
                )
            }
        }

        // Standard number of columns in row 1 (10 for QWERTY/QWERTZ/AZERTY)
        val standardCols = maxOf(10, rows[0].size)
        val totalRowWidth = effectiveWidth - (outerHorizontalMargin * 2)
        val totalStandardGaps = (standardCols - 1) * horizontalKeyGap
        val baseKeyWidth = (totalRowWidth - totalStandardGaps) / standardCols

        var currentY = topPadding

        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            if (row.isEmpty()) continue

            when (rowIndex) {
                0 -> {
                    // Row 1: standard characters (e.g. Q W E R T Y U I O P)
                    val colsInRow = row.size
                    val keyW = if (colsInRow == standardCols) baseKeyWidth
                    else (totalRowWidth - (colsInRow - 1) * horizontalKeyGap) / colsInRow

                    var currentX = startXOffset + outerHorizontalMargin
                    for (key in row) {
                        setKeyBounds(key, currentX, currentY, keyW, rowHeight, horizontalKeyGap, verticalRowGap)
                        currentX += keyW + horizontalKeyGap
                    }
                }
                1 -> {
                    // Row 2: centered with half-key indent (e.g. A S D F G H J K L)
                    val colsInRow = row.size
                    if (colsInRow < standardCols) {
                        val rowContentWidth = (colsInRow * baseKeyWidth) + ((colsInRow - 1) * horizontalKeyGap)
                        val indent = (totalRowWidth - rowContentWidth) / 2f
                        var currentX = startXOffset + outerHorizontalMargin + indent
                        for (key in row) {
                            setKeyBounds(key, currentX, currentY, baseKeyWidth, rowHeight, horizontalKeyGap, verticalRowGap)
                            currentX += baseKeyWidth + horizontalKeyGap
                        }
                    } else {
                        val keyW = (totalRowWidth - (colsInRow - 1) * horizontalKeyGap) / colsInRow
                        var currentX = startXOffset + outerHorizontalMargin
                        for (key in row) {
                            setKeyBounds(key, currentX, currentY, keyW, rowHeight, horizontalKeyGap, verticalRowGap)
                            currentX += keyW + horizontalKeyGap
                        }
                    }
                }
                2 -> {
                    // Row 3: Shift on left, Delete on right, characters centered
                    if (row.size >= 3) {
                        val leftKey = row.first()
                        val rightKey = row.last()
                        val middleKeys = row.subList(1, row.size - 1)

                        val middleCols = middleKeys.size
                        val middleKeysWidth = (middleCols * baseKeyWidth) + ((middleCols - 1) * horizontalKeyGap)
                        val remainingWidth = totalRowWidth - middleKeysWidth - (2 * horizontalKeyGap)
                        val modifierKeyWidth = (remainingWidth / 2f).coerceAtLeast(baseKeyWidth * 1.25f)

                        var currentX = startXOffset + outerHorizontalMargin
                        // Left modifier (Shift)
                        setKeyBounds(leftKey, currentX, currentY, modifierKeyWidth, rowHeight, horizontalKeyGap, verticalRowGap)
                        currentX += modifierKeyWidth + horizontalKeyGap

                        // Middle characters (Z X C V B N M)
                        for (key in middleKeys) {
                            setKeyBounds(key, currentX, currentY, baseKeyWidth, rowHeight, horizontalKeyGap, verticalRowGap)
                            currentX += baseKeyWidth + horizontalKeyGap
                        }

                        // Right modifier (Delete)
                        setKeyBounds(rightKey, currentX, currentY, modifierKeyWidth, rowHeight, horizontalKeyGap, verticalRowGap)
                    } else {
                        distributeRowEvenly(row, startXOffset + outerHorizontalMargin, currentY, totalRowWidth, rowHeight, horizontalKeyGap, verticalRowGap)
                    }
                }
                else -> {
                    // Row 4: 123, Emoji, Space, Return
                    measureBottomRow(row, startXOffset + outerHorizontalMargin, currentY, totalRowWidth, rowHeight, baseKeyWidth, horizontalKeyGap, verticalRowGap)
                }
            }

            currentY += rowHeight + verticalRowGap
        }

        // Measure floating bottom bar buttons (Globe on left, Dictation on right)
        // Positioned cleanly in the floating bar ABOVE the system navigation bar
        val iconTouchW = 44f * density
        val iconTouchH = 38f * density
        val bottomIconCenterY = viewHeight - bottomInset - (floatingBarHeight / 2f)

        val globeCenterX = startXOffset + outerHorizontalMargin + (22f * density)
        globeButtonBounds.set(
            globeCenterX - (iconTouchW / 2f),
            bottomIconCenterY - (iconTouchH / 2f),
            globeCenterX + (iconTouchW / 2f),
            bottomIconCenterY + (iconTouchH / 2f)
        )
        globeKeyDefinition.bounds.set(globeButtonBounds)
        globeKeyDefinition.touchBounds.set(globeButtonBounds)

        val dictationCenterX = (startXOffset + effectiveWidth) - outerHorizontalMargin - (22f * density)
        dictationButtonBounds.set(
            dictationCenterX - (iconTouchW / 2f),
            bottomIconCenterY - (iconTouchH / 2f),
            dictationCenterX + (iconTouchW / 2f),
            bottomIconCenterY + (iconTouchH / 2f)
        )
        dictationKeyDefinition.bounds.set(dictationButtonBounds)
        dictationKeyDefinition.touchBounds.set(dictationButtonBounds)
    }

    private fun measureBottomRow(
        row: List<KeyDefinition>,
        startX: Float,
        currentY: Float,
        totalRowWidth: Float,
        rowHeight: Float,
        baseKeyWidth: Float,
        horizontalKeyGap: Float,
        verticalRowGap: Float
    ) {
        val spaceKey = row.firstOrNull { it.keyType == KeyType.SPACE }
        if (spaceKey == null) {
            distributeRowEvenly(row, startX, currentY, totalRowWidth, rowHeight, horizontalKeyGap, verticalRowGap)
            return
        }

        // Pre-determine specific widths for known iOS bottom keys
        val keyWidthMap = mutableMapOf<KeyDefinition, Float>()
        for (key in row) {
            when (key.keyType) {
                KeyType.SWITCH_NUMERIC, KeyType.SWITCH_ALPHA -> keyWidthMap[key] = (baseKeyWidth * 1.35f)
                KeyType.EMOJI -> keyWidthMap[key] = (baseKeyWidth * 1.15f)
                KeyType.GLOBE -> keyWidthMap[key] = (baseKeyWidth * 1.15f)
                KeyType.DICTATION -> keyWidthMap[key] = (baseKeyWidth * 1.0f)
                KeyType.RETURN -> keyWidthMap[key] = (baseKeyWidth * 1.85f)
                KeyType.SPACE -> {} // Computed dynamically
                else -> keyWidthMap[key] = (baseKeyWidth * 1.0f)
            }
        }

        val totalGaps = (row.size - 1) * horizontalKeyGap
        val otherKeysWidth = keyWidthMap.values.sum()
        val availableForSpace = totalRowWidth - otherKeysWidth - totalGaps
        val calculatedSpaceWidth = availableForSpace.coerceAtLeast(baseKeyWidth * 2.5f)
        keyWidthMap[spaceKey] = calculatedSpaceWidth

        var currentX = startX
        for (key in row) {
            val w = keyWidthMap[key] ?: baseKeyWidth
            setKeyBounds(key, currentX, currentY, w, rowHeight, horizontalKeyGap, verticalRowGap)
            currentX += w + horizontalKeyGap
        }
    }

    private fun distributeRowEvenly(
        row: List<KeyDefinition>,
        startX: Float,
        currentY: Float,
        totalRowWidth: Float,
        rowHeight: Float,
        horizontalKeyGap: Float,
        verticalRowGap: Float
    ) {
        val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
        val totalGaps = (row.size - 1) * horizontalKeyGap
        val unitW = (totalRowWidth - totalGaps) / totalWeight
        var currentX = startX
        for (key in row) {
            val keyW = key.weight * unitW
            setKeyBounds(key, currentX, currentY, keyW, rowHeight, horizontalKeyGap, verticalRowGap)
            currentX += keyW + horizontalKeyGap
        }
    }

    private fun setKeyBounds(
        key: KeyDefinition,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        gapX: Float,
        gapY: Float
    ) {
        key.bounds.set(left, top, left + width, top + height)
        key.touchBounds.set(
            left - (gapX / 2f),
            top - (gapY / 2f),
            left + width + (gapX / 2f),
            top + height + (gapY / 2f)
        )
    }

    fun findKeyAt(x: Float, y: Float): KeyDefinition? {
        if (globeButtonBounds.contains(x, y)) {
            return globeKeyDefinition
        }
        if (dictationButtonBounds.contains(x, y)) {
            return dictationKeyDefinition
        }
        for (row in rows) {
            for (key in row) {
                if (key.touchBounds.contains(x, y)) {
                    return key
                }
            }
        }
        return null
    }
}
