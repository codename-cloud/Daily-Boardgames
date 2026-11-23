package com.example.minesweeper.audio

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SoundManager {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playClick() { playTone(ToneGenerator.TONE_PROP_BEEP, 35) }
    fun playFlag() { playTone(ToneGenerator.TONE_PROP_ACK, 60) }
    fun playChord() { playTone(ToneGenerator.TONE_PROP_PROMPT, 50) }
    fun playExplosion() { playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 250) }

    fun playVictory(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            playTone(ToneGenerator.TONE_PROP_BEEP2, 100)
            delay(120)
            playTone(ToneGenerator.TONE_PROP_ACK, 100)
            delay(120)
            playTone(ToneGenerator.TONE_PROP_PROMPT, 200)
        }
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
