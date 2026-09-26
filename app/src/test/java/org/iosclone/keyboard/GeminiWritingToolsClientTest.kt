package org.iosclone.keyboard

import org.iosclone.keyboard.writingtools.WritingToolsTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiWritingToolsClientTest {

    @Test
    fun testMarkdownResponseCleaning() {
        fun cleanResponse(response: String): String {
            var cleaned = response.trim()
            if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
                val lines = cleaned.lines()
                if (lines.size >= 2) {
                    cleaned = lines.subList(1, lines.size - 1).joinToString("\n").trim()
                }
            }
            return cleaned
        }

        val rawWithCodeblock = "```\nThis is a proofread sentence.\n```"
        val cleaned = cleanResponse(rawWithCodeblock)
        assertEquals("This is a proofread sentence.", cleaned)

        val rawPlain = "Already clean text without fences."
        assertEquals("Already clean text without fences.", cleanResponse(rawPlain))
    }

    @Test
    fun testWritingToolsTaskCoverage() {
        val tasks = WritingToolsTask.entries.toTypedArray()
        assertTrue(tasks.contains(WritingToolsTask.PROOFREAD))
        assertTrue(tasks.contains(WritingToolsTask.REWRITE))
        assertTrue(tasks.contains(WritingToolsTask.FRIENDLY))
        assertTrue(tasks.contains(WritingToolsTask.PROFESSIONAL))
        assertTrue(tasks.contains(WritingToolsTask.CONCISE))
        assertTrue(tasks.contains(WritingToolsTask.SUMMARY))
        assertTrue(tasks.contains(WritingToolsTask.KEY_POINTS))
        assertTrue(tasks.contains(WritingToolsTask.LIST))
        assertTrue(tasks.contains(WritingToolsTask.TABLE))
        assertTrue(tasks.contains(WritingToolsTask.COMPOSE))
        assertTrue(tasks.contains(WritingToolsTask.CUSTOM))
    }
}
