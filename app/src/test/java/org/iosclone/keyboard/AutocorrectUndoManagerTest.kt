package org.iosclone.keyboard

import org.iosclone.keyboard.dictionary.AutocorrectUndoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutocorrectUndoManagerTest {

    @Test
    fun testUndoOnBackspace() {
        val undoManager = AutocorrectUndoManager()

        // User typed "teh", autocorrected to "the"
        undoManager.recordReplacement("teh", "the")

        // User immediately presses backspace right after the replaced word with trailing space
        val record = undoManager.shouldUndoOnDelete("I saw the ")
        assertNotNull(record)
        assertEquals("teh", record?.originalTyped)

        // After undo, "teh" is temporarily marked ignored so typing space doesn't immediately re-correct
        assertTrue(undoManager.isIgnored("teh"))
    }

    @Test
    fun testUndoDoesNotTriggerUnrelated() {
        val undoManager = AutocorrectUndoManager()
        undoManager.recordReplacement("teh", "the")

        // User typed something else
        val record = undoManager.shouldUndoOnDelete("Something completely different")
        assertNull(record)
    }
}
