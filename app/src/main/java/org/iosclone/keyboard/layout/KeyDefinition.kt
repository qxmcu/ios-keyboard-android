package org.iosclone.keyboard.layout

import android.graphics.RectF

data class KeyDefinition(
    val code: Int,
    val label: String,
    val secondaryLabel: String? = null,
    val keyType: KeyType = KeyType.CHARACTER,
    val weight: Float = 1.0f,
    val accents: List<String> = emptyList()
) {
    // Runtime computed bounds on Canvas
    val bounds: RectF = RectF()
    val touchBounds: RectF = RectF() // expanded touch target

    fun isModifier(): Boolean {
        return keyType != KeyType.CHARACTER && keyType != KeyType.SPACE
    }
}
