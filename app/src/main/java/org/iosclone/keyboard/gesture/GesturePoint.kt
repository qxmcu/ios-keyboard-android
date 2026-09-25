package org.iosclone.keyboard.gesture

data class GesturePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)
