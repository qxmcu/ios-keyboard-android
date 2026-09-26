package org.iosclone.keyboard

import org.iosclone.keyboard.dictation.WhisperCppEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperCppEngineTest {

    @Test
    fun testAudioFrameConversion() {
        val pcm16Buffer = shortArrayOf(0, 16384, -16384, 32767, -32768)
        val floatSamples = FloatArray(pcm16Buffer.size)
        for (i in pcm16Buffer.indices) {
            floatSamples[i] = pcm16Buffer[i] / 32768.0f
        }

        assertEquals(0.0f, floatSamples[0], 0.001f)
        assertEquals(0.5f, floatSamples[1], 0.001f)
        assertEquals(-0.5f, floatSamples[2], 0.001f)
        assertEquals(1.0f, floatSamples[3], 0.01f)
        assertEquals(-1.0f, floatSamples[4], 0.01f)
    }

    @Test
    fun testModelTypeEnum() {
        assertEquals("ggml-tiny.en.bin", WhisperCppEngine.ModelType.WHISPER_TINY_EN.fileName)
        assertEquals("ggml-base.en.bin", WhisperCppEngine.ModelType.WHISPER_BASE_EN.fileName)
        assertTrue(WhisperCppEngine.ModelType.WHISPER_TINY_EN.approxSizeBytes > 30_000_000L)
    }
}
