package com.example.minesweeper.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticStrength(val label: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Strong")
}

class HapticManager(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun performFlagHaptic(strength: HapticStrength = HapticStrength.MEDIUM) {
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = when (strength) {
                    HapticStrength.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    HapticStrength.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    HapticStrength.STRONG -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (duration, amplitude) = when (strength) {
                    HapticStrength.LIGHT -> 20L to 70
                    HapticStrength.MEDIUM -> 35L to 150
                    HapticStrength.STRONG -> 60L to 255
                }
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                val duration = when (strength) {
                    HapticStrength.LIGHT -> 20L
                    HapticStrength.MEDIUM -> 35L
                    HapticStrength.STRONG -> 60L
                }
                vibrator.vibrate(duration)
            }
        } catch (_: Exception) {}
    }

    fun performRevealHaptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15L, 60))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15L)
            }
        } catch (_: Exception) {}
    }

    fun performExplosionHaptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 80, 50, 150)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 80, 50, 150), -1)
            }
        } catch (_: Exception) {}
    }
}
