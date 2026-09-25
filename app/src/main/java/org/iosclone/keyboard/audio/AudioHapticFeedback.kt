package org.iosclone.keyboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Ultra-low latency Audio and Haptic feedback pipeline mimicking the Apple Taptic Engine.
 */
class AudioHapticFeedback(private val context: Context) {

    private val tag = "AudioHapticFeedback"

    private var soundPool: SoundPool? = null
    private var standardSoundId: Int = 0
    private var deleteSoundId: Int = 0
    private var returnSoundId: Int = 0

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    init {
        initializeAudio()
    }

    private fun initializeAudio() {
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attributes)
                .build()

            // Load sounds from assets or raw resources
            loadSounds()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize SoundPool", e)
        }
    }

    private fun loadSounds() {
        val pool = soundPool ?: return

        try {
            // First check if raw resources exist
            val rawClickId = context.resources.getIdentifier("key_click", "raw", context.packageName)
            val rawDeleteId = context.resources.getIdentifier("key_delete", "raw", context.packageName)
            val rawReturnId = context.resources.getIdentifier("key_return", "raw", context.packageName)

            if (rawClickId != 0) {
                standardSoundId = pool.load(context, rawClickId, 1)
            } else {
                context.assets.openFd("sounds/key_click.wav").use { fd ->
                    standardSoundId = pool.load(fd, 1)
                }
            }

            if (rawDeleteId != 0) {
                deleteSoundId = pool.load(context, rawDeleteId, 1)
            } else {
                context.assets.openFd("sounds/key_delete.wav").use { fd ->
                    deleteSoundId = pool.load(fd, 1)
                }
            }

            if (rawReturnId != 0) {
                returnSoundId = pool.load(context, rawReturnId, 1)
            } else {
                context.assets.openFd("sounds/key_return.wav").use { fd ->
                    returnSoundId = pool.load(fd, 1)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load audio files from assets, falling back to system clicks: ${e.message}")
        }
    }

    /**
     * Plays authentic iOS keystroke sound with low-latency SoundPool.
     */
    fun playKeystrokeSound(type: SoundType, enabled: Boolean, volumePercent: Int) {
        if (!enabled || volumePercent <= 0) return

        val pool = soundPool
        val volume = (volumePercent.coerceIn(0, 100) / 100f) * 0.9f

        val soundId = when (type) {
            SoundType.STANDARD -> standardSoundId
            SoundType.DELETE -> deleteSoundId
            SoundType.RETURN_SPACE -> returnSoundId
        }

        if (pool != null && soundId != 0) {
            pool.play(soundId, volume, volume, 1, 0, 1.0f)
        } else {
            // Graceful fallback to Android AudioManager click
            val effect = when (type) {
                SoundType.STANDARD -> AudioManager.FX_KEYPRESS_STANDARD
                SoundType.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                SoundType.RETURN_SPACE -> AudioManager.FX_KEYPRESS_RETURN
            }
            audioManager?.playSoundEffect(effect, volume)
        }
    }

    /**
     * Replicates the Apple Taptic Engine micro-click tactile sensation.
     * @param intensityPercent 1..100%
     */
    fun performHapticFeedback(type: SoundType, enabled: Boolean, intensityPercent: Int) {
        if (!enabled || intensityPercent <= 0) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val scale = intensityPercent.coerceIn(1, 100) / 100f
        // Amplitude range: 1..255
        val baseAmplitude = (255 * scale).toInt().coerceIn(1, 255)

        val (durationMs, amplitude) = when (type) {
            SoundType.STANDARD -> Pair(10L, (baseAmplitude * 0.7f).toInt().coerceAtLeast(1))
            SoundType.DELETE -> Pair(12L, (baseAmplitude * 0.85f).toInt().coerceAtLeast(1))
            SoundType.RETURN_SPACE -> Pair(14L, baseAmplitude)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+, use EFFECT_CLICK if available or tailored one-shot
                val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(tag, "Vibration failed", e)
        }
    }

    /**
     * Soft bump for long-press trigger
     */
    fun performLongPressHaptic(enabled: Boolean, intensityPercent: Int) {
        if (!enabled || intensityPercent <= 0) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val scale = intensityPercent.coerceIn(1, 100) / 100f
        val amplitude = (200 * scale).toInt().coerceIn(1, 255)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(20L, amplitude)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(20L)
            }
        } catch (e: Exception) {
            Log.e(tag, "Long press vibration failed", e)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
