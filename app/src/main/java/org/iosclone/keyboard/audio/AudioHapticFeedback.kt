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
import org.iosclone.keyboard.R

/**
 * Ultra-low latency Audio and Haptic feedback pipeline mimicking the Apple Taptic Engine.
 * Fully optimized for Nothing OS (Nothing Phone), Samsung One UI, and Pixel haptic engines.
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
                .setMaxStreams(10)
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
            // Direct static R.raw reference to guarantee fast loading without reflection
            standardSoundId = pool.load(context, R.raw.key_click, 1)
            deleteSoundId = pool.load(context, R.raw.key_delete, 1)
            returnSoundId = pool.load(context, R.raw.key_return, 1)
        } catch (e: Exception) {
            Log.w(tag, "Failed to load audio from raw resources: ${e.message}")
            try {
                val clickFd = context.assets.openFd("sounds/key_click.wav")
                standardSoundId = pool.load(clickFd, 1)
                val deleteFd = context.assets.openFd("sounds/key_delete.wav")
                deleteSoundId = pool.load(deleteFd, 1)
                val returnFd = context.assets.openFd("sounds/key_return.wav")
                returnSoundId = pool.load(returnFd, 1)
            } catch (e2: Exception) {
                Log.w(tag, "Failed to load audio from assets: ${e2.message}")
            }
        }
    }

    /**
     * Plays authentic iOS keystroke sound with low-latency SoundPool, View, or AudioManager.
     */
    fun playKeystrokeSound(type: SoundType, enabled: Boolean, volumePercent: Int, view: View? = null) {
        if (!enabled || volumePercent <= 0) return

        val pool = soundPool
        val volume = (volumePercent.coerceIn(0, 100) / 100f)

        val soundId = when (type) {
            SoundType.STANDARD -> standardSoundId
            SoundType.DELETE -> deleteSoundId
            SoundType.RETURN_SPACE -> returnSoundId
        }

        var played = false
        if (pool != null && soundId != 0) {
            try {
                val streamId = pool.play(soundId, volume, volume, 1, 0, 1.0f)
                if (streamId != 0) played = true
            } catch (e: Exception) {
                played = false
            }
        }

        if (!played) {
            // View sound effect fallback
            try {
                view?.playSoundEffect(SoundEffectConstants.CLICK)
            } catch (ignored: Exception) {}

            // AudioManager fallback
            try {
                val effect = when (type) {
                    SoundType.STANDARD -> AudioManager.FX_KEYPRESS_STANDARD
                    SoundType.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                    SoundType.RETURN_SPACE -> AudioManager.FX_KEYPRESS_RETURN
                }
                audioManager?.playSoundEffect(effect, volume)
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Replicates the Apple Taptic Engine micro-click tactile sensation.
     * Guaranteed tactile feedback on Nothing Phone, Samsung Galaxy, and Google Pixel.
     */
    fun performHapticFeedback(type: SoundType, enabled: Boolean, intensityPercent: Int, view: View? = null) {
        if (!enabled || intensityPercent <= 0) return

        // 1. Direct View Haptic Feedback (bypasses device touch feedback toggles)
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
                view.performHapticFeedback(constant, flag)
            } catch (ignored: Exception) {}
        }

        // 2. Hardware Vibrator pipeline - uses createOneShot to guarantee sharp LRA pulse on Nothing OS
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            val scale = intensityPercent.coerceIn(1, 100) / 100f
            val (durationMs, amplitude) = when (type) {
                SoundType.STANDARD -> Pair(14L, (180 * scale).toInt().coerceIn(1, 255))
                SoundType.DELETE -> Pair(16L, (210 * scale).toInt().coerceIn(1, 255))
                SoundType.RETURN_SPACE -> Pair(20L, (240 * scale).toInt().coerceIn(1, 255))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var effect: VibrationEffect? = null
                // Attempt predefined effect if supported by OEM HAL (e.g. Pixel), otherwise fallback to createOneShot
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val predefined = when (type) {
                            SoundType.STANDARD -> VibrationEffect.EFFECT_TICK
                            SoundType.DELETE -> VibrationEffect.EFFECT_CLICK
                            SoundType.RETURN_SPACE -> VibrationEffect.EFFECT_CLICK
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vib.areAllEffectsSupported(predefined) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) {
                            effect = VibrationEffect.createPredefined(predefined)
                        }
                    } catch (ignored: Exception) {}
                }

                if (effect == null) {
                    effect = VibrationEffect.createOneShot(durationMs, amplitude)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
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
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(tag, "Vibration failed", e)
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
            } catch (ignored: Exception) {}
        }

        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            val scale = intensityPercent.coerceIn(1, 100) / 100f
            val amplitude = (255 * scale).toInt().coerceIn(1, 255)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var effect: VibrationEffect? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vib.areAllEffectsSupported(VibrationEffect.EFFECT_HEAVY_CLICK) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) {
                            effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                        }
                    } catch (ignored: Exception) {}
                }
                if (effect == null) {
                    effect = VibrationEffect.createOneShot(32L, amplitude)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
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
                @Suppress("DEPRECATION")
                vib.vibrate(32L)
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
