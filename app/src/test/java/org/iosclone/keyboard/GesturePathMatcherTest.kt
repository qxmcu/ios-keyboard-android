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
    fun testGesturePathMatching() {
        val matcher = GesturePathMatcher()
        val layout = KeyboardLayoutFactory.createLayout(LanguageLayout.QWERTY, KeyboardMode.LOWERCASE)
        // Measure layout so bounds are computed
        layout.measure(1080f, 800f, 2.75f)

        val trie = Trie().apply {
            insert("hi", 150)
            insert("hello", 200)
            insert("help", 180)
            insert("here", 170)
        }

        // Find keys 'h' and 'i'
        val hKey = layout.rows.flatten().first { it.label == "h" }
        val iKey = layout.rows.flatten().first { it.label == "i" }

        val points = listOf(
            GesturePoint(hKey.bounds.centerX(), hKey.bounds.centerY(), 0L),
            GesturePoint((hKey.bounds.centerX() + iKey.bounds.centerX()) / 2f, (hKey.bounds.centerY() + iKey.bounds.centerY()) / 2f, 50L),
            GesturePoint(iKey.bounds.centerX(), iKey.bounds.centerY(), 100L)
        )

        val matches = matcher.match(points, layout, trie)
        assertTrue("Matches should contain 'hi', got: $matches", matches.contains("hi"))
    }
}
