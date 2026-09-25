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
     * Dynamically calculates bounds and touch targets for each key across all rows.
     */
    fun measure(
        viewWidth: Float,
        viewHeight: Float,
        density: Float,
        bottomInset: Float = 0f
    ) {
        if (rows.isEmpty() || viewWidth <= 0 || viewHeight <= 0) return

        val horizontalKeyGap = 6f * density
        val verticalRowGap = 10f * density
        val outerHorizontalMargin = 4f * density
        val topPadding = 8f * density
        val bottomPadding = (4f * density) + bottomInset

        val numRows = rows.size
        val availableHeight = viewHeight - topPadding - bottomPadding - (verticalRowGap * (numRows - 1))
        val rowHeight = (availableHeight / numRows).coerceAtLeast(36f * density)

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
                val sideWidth = viewWidth - effectiveWidth
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

        var currentY = topPadding

        for (row in rows) {
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val totalGaps = (row.size - 1) * horizontalKeyGap
            val availableRowWidth = effectiveWidth - (outerHorizontalMargin * 2) - totalGaps
            val unitWeightWidth = availableRowWidth / totalWeight

            var currentX = startXOffset + outerHorizontalMargin

            for (key in row) {
                val keyWidth = key.weight * unitWeightWidth
                key.bounds.set(
                    currentX,
                    currentY,
                    currentX + keyWidth,
                    currentY + rowHeight
                )

                // Touch bounds with generous half-gap extension for zero-miss typing
                key.touchBounds.set(
                    currentX - (horizontalKeyGap / 2f),
                    currentY - (verticalRowGap / 2f),
                    currentX + keyWidth + (horizontalKeyGap / 2f),
                    currentY + rowHeight + (verticalRowGap / 2f)
                )

                currentX += keyWidth + horizontalKeyGap
            }

            currentY += rowHeight + verticalRowGap
        }
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
