package org.iosclone.keyboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View

/**
 * Ultra-low latency Audio and Haptic feedback pipeline mimicking the Apple Taptic Engine.
 */
class AudioHapticFeedback(private val context: Context) {

    private val tag = "AudioHapticFeedback"

    private var soundPool: SoundPool? = null
    private var standardSoundId: Int = 0
    private var deleteSoundId: Int = 0
    private var returnSoundId: Int = 0
    private val loadedSoundIds = mutableSetOf<Int>()

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
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            loadedSoundIds.add(sampleId)
                        }
                    }
                }

            loadSounds()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize SoundPool", e)
        }
    }

    private fun loadSounds() {
        val pool = soundPool ?: return

        try {
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
            Log.w(tag, "Failed to load audio files: ${e.message}")
        }
    }

    /**
     * Plays authentic iOS keystroke sound with low-latency SoundPool, View, or AudioManager.
     */
    fun playKeystrokeSound(type: SoundType, enabled: Boolean, volumePercent: Int, view: View? = null) {
        if (!enabled || volumePercent <= 0) return

        val pool = soundPool
        val volume = (volumePercent.coerceIn(0, 100) / 100f) * 0.9f

        val soundId = when (type) {
            SoundType.STANDARD -> standardSoundId
            SoundType.DELETE -> deleteSoundId
            SoundType.RETURN_SPACE -> returnSoundId
        }

        var played = false
        if (pool != null && soundId != 0 && loadedSoundIds.contains(soundId)) {
            val streamId = pool.play(soundId, volume, volume, 1, 0, 1.0f)
            if (streamId != 0) played = true
        }

        if (!played) {
            // View sound effect
            try {
                view?.playSoundEffect(SoundEffectConstants.CLICK)
            } catch (e: Exception) {
                // Ignore
            }

            // AudioManager fallback
            try {
                val effect = when (type) {
                    SoundType.STANDARD -> AudioManager.FX_KEYPRESS_STANDARD
                    SoundType.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                    SoundType.RETURN_SPACE -> AudioManager.FX_KEYPRESS_RETURN
                }
                audioManager?.playSoundEffect(effect, volume)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Replicates the Apple Taptic Engine micro-click tactile sensation.
     * Uses View.performHapticFeedback with FLAG_IGNORE_GLOBAL_SETTING as primary,
     * with direct Vibrator IME feedback as hardware fallback.
     */
    fun performHapticFeedback(type: SoundType, enabled: Boolean, intensityPercent: Int, view: View? = null) {
        if (!enabled || intensityPercent <= 0) return

        // 1. Direct View Haptic Feedback (bypasses device touch feedback toggles)
        var viewHapticSuccess = false
        if (view != null) {
            try {
                val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                } else {
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                }
                val constant = when (type) {
                    SoundType.STANDARD -> HapticFeedbackConstants.KEYBOARD_TAP
                    SoundType.DELETE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) HapticFeedbackConstants.KEYBOARD_RELEASE else HapticFeedbackConstants.KEYBOARD_TAP
                    SoundType.RETURN_SPACE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) HapticFeedbackConstants.KEYBOARD_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
                }
                viewHapticSuccess = view.performHapticFeedback(constant, flag)
            } catch (e: Exception) {
                viewHapticSuccess = false
            }
        }

        // 2. Hardware Vibrator pipeline
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = when (type) {
                    SoundType.STANDARD -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    SoundType.DELETE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    SoundType.RETURN_SPACE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_IME_FEEDBACK)
                        .build()
                    vib.vibrate(effect, attrs)
                } else {
                    val audioAttrs = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                    vib.vibrate(effect, audioAttrs)
                }
            } else {
                val scale = intensityPercent.coerceIn(1, 100) / 100f
                val (durationMs, amplitude) = when (type) {
                    SoundType.STANDARD -> Pair(12L, (200 * scale).toInt().coerceIn(1, 255))
                    SoundType.DELETE -> Pair(15L, (225 * scale).toInt().coerceIn(1, 255))
                    SoundType.RETURN_SPACE -> Pair(18L, (255 * scale).toInt().coerceIn(1, 255))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            if (!viewHapticSuccess) {
                Log.e(tag, "Vibration failed", e)
            }
        }
    }

    /**
     * Firm bump for long-press trigger.
     */
    fun performLongPressHaptic(enabled: Boolean, intensityPercent: Int, view: View? = null) {
        if (!enabled || intensityPercent <= 0) return

        if (view != null) {
            try {
                val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                } else {
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                }
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, flag)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_IME_FEEDBACK)
                        .build()
                    vib.vibrate(effect, attrs)
                } else {
                    vib.vibrate(effect)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val scale = intensityPercent.coerceIn(1, 100) / 100f
                val amplitude = (255 * scale).toInt().coerceIn(1, 255)
                val effect = VibrationEffect.createOneShot(30L, amplitude)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(30L)
            }
        } catch (e: Exception) {
            Log.e(tag, "Long press vibration failed", e)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loadedSoundIds.clear()
    }
}
