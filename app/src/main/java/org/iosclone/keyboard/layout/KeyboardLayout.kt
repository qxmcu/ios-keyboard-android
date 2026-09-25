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

    /**
     * Dynamically calculates bounds and touch targets for each key across all rows
     * with authentic iOS proportions and alignment.
     */
    fun measure(
        viewWidth: Float,
        viewHeight: Float,
        density: Float,
        bottomInset: Float = 0f
    ) {
        if (rows.isEmpty() || viewWidth <= 0 || viewHeight <= 0) return

        val horizontalKeyGap = 6f * density
        val verticalRowGap = 11f * density
        val outerHorizontalMargin = 4f * density
        val topPadding = 8f * density
        val bottomPadding = 8f * density + bottomInset

        val numRows = rows.size
        val availableHeight = viewHeight - topPadding - bottomPadding - (verticalRowGap * (numRows - 1))
        val rowHeight = (availableHeight / numRows).coerceAtLeast(38f * density)

        // One-handed geometry calculation
        val oneHandedFactor = 0.82f
        val effectiveWidth: Float
        val startXOffset: Float

        when (oneHandedMode) {
            OneHandedMode.NORMAL -> {
                effectiveWidth = viewWidth
                startXOffset = 0f
                oneHandedSideButtonBounds.setEmpty()
            }
            OneHandedMode.LEFT_DOCKED -> {
                effectiveWidth = viewWidth * oneHandedFactor
                startXOffset = 0f
                oneHandedSideButtonBounds.set(
                    effectiveWidth,
                    topPadding,
                    viewWidth,
                    viewHeight - bottomPadding
                )
            }
            OneHandedMode.RIGHT_DOCKED -> {
                effectiveWidth = viewWidth * oneHandedFactor
                startXOffset = viewWidth - effectiveWidth
                oneHandedSideButtonBounds.set(
                    0f,
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
                        // Fallback distribution
                        distributeRowEvenly(row, startXOffset + outerHorizontalMargin, currentY, totalRowWidth, rowHeight, horizontalKeyGap, verticalRowGap)
                    }
                }
                else -> {
                    // Row 4: 123, Globe, Mic, Space, Return
                    measureBottomRow(row, startXOffset + outerHorizontalMargin, currentY, totalRowWidth, rowHeight, baseKeyWidth, horizontalKeyGap, verticalRowGap)
                }
            }

            currentY += rowHeight + verticalRowGap
        }
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
                KeyType.GLOBE -> keyWidthMap[key] = (baseKeyWidth * 1.05f)
                KeyType.DICTATION -> keyWidthMap[key] = (baseKeyWidth * 0.95f)
                KeyType.RETURN -> keyWidthMap[key] = (baseKeyWidth * 2.05f)
                KeyType.SPACE -> {} // Computed dynamically
                else -> keyWidthMap[key] = (baseKeyWidth * 1.0f)
            }
        }

        val totalGaps = (row.size - 1) * horizontalKeyGap
        val otherKeysWidth = keyWidthMap.values.sum()
        val calculatedSpaceWidth = (totalRowWidth - otherKeysWidth - totalGaps).coerceAtLeast(baseKeyWidth * 2.5f)
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
