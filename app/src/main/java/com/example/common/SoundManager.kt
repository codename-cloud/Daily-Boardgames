package com.example.common

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class SoundManager {
    private val audioExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var toneGenerator: ToneGenerator? = null

    @Volatile
    private var released = false

    private var lastToneMs = 0L

    init {
        audioExecutor.execute {
            if (released) return@execute
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            } catch (_: Exception) {
                toneGenerator = null
            }
        }
    }

    fun playClick() { playTone(ToneGenerator.TONE_PROP_BEEP, 35) }
    fun playFlag() { playTone(ToneGenerator.TONE_PROP_ACK, 60) }
    fun playChord() { playTone(ToneGenerator.TONE_PROP_PROMPT, 50) }
    fun playExplosion() { playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 250, minIntervalMs = 0L) }

    fun playVictory(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            playTone(ToneGenerator.TONE_PROP_BEEP2, 100, minIntervalMs = 0L)
            delay(120)
            playTone(ToneGenerator.TONE_PROP_ACK, 100, minIntervalMs = 0L)
            delay(120)
            playTone(ToneGenerator.TONE_PROP_PROMPT, 200, minIntervalMs = 0L)
        }
    }

    private fun playTone(toneType: Int, durationMs: Int, minIntervalMs: Long = 55L) {
        if (released) return
        audioExecutor.execute {
            if (released) return@execute
            val now = System.currentTimeMillis()
            if (minIntervalMs > 0L && now - lastToneMs < minIntervalMs) return@execute
            lastToneMs = now
            try {
                toneGenerator?.startTone(toneType, durationMs)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        released = true
        try {
            audioExecutor.execute {
                try {
                    toneGenerator?.release()
                    toneGenerator = null
                } catch (_: Exception) {}
            }
            audioExecutor.shutdown()
        } catch (_: Exception) {}
    }
}
