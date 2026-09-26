package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.Trie
import org.iosclone.keyboard.gesture.GesturePathMatcher
import org.iosclone.keyboard.gesture.GesturePoint
import org.iosclone.keyboard.layout.KeyboardLayoutFactory
import org.iosclone.keyboard.layout.KeyboardMode
import org.iosclone.keyboard.layout.LanguageLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturePathMatcherTest {

    @Test
    fun testEmptyOrTooShortPoints() {
        val matcher = GesturePathMatcher()
        val layout = KeyboardLayoutFactory.createLayout(LanguageLayout.QWERTY, KeyboardMode.LOWERCASE)
        val trie = Trie().apply {
            insert("hello", 100)
        }

        val emptyResult = matcher.match(emptyList(), layout, trie)
        assertTrue(emptyResult.isEmpty())

        val shortResult = matcher.match(listOf(GesturePoint(10f, 10f, 0L), GesturePoint(20f, 20f, 10L)), layout, trie)
        assertTrue(shortResult.isEmpty())
    }

    @Test
    fun testNullTrieReturnsEmpty() {
        val matcher = GesturePathMatcher()
        val layout = KeyboardLayoutFactory.createLayout(LanguageLayout.QWERTY, KeyboardMode.LOWERCASE)
        val points = listOf(
            GesturePoint(10f, 10f, 0L),
            GesturePoint(20f, 20f, 10L),
            GesturePoint(30f, 30f, 20L),
            GesturePoint(40f, 40f, 30L)
        )
        val result = matcher.match(points, layout, null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testNumericModeInsufficientPoints() {
        val matcher = GesturePathMatcher()
        val layout = KeyboardLayoutFactory.createLayout(LanguageLayout.QWERTY, KeyboardMode.NUMERIC)
        val result = matcher.match(listOf(GesturePoint(10f, 10f, 0L), GesturePoint(20f, 20f, 50L)), layout, null)
        assertTrue(result.isEmpty())
    }
}
