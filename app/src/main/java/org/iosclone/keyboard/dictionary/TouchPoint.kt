package org.iosclone.keyboard.dictionary

/**
 * Represents a physical touch event on the keyboard canvas.
 * Encapsulates the character key intended, along with exact physical canvas pixel coordinates
 * to compute spatial Euclidean deviation penalties during autocorrect.
 */
data class TouchPoint(
    val char: Char,
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class KeyCenter(
    val x: Float,
    val y: Float
)
