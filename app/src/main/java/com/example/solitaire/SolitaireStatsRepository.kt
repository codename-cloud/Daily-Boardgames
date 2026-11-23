package com.example.solitaire

import android.content.Context
import android.content.SharedPreferences

class SolitaireStatsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("solitaire_stats_prefs", Context.MODE_PRIVATE)

    fun getStats(): SolitaireStats {
        val played = prefs.getInt("played", 0)
        val won = prefs.getInt("won", 0)
        val bestTime = prefs.getInt("best_time", -1).let { if (it > 0) it else null }
        val bestMoves = prefs.getInt("best_moves", -1).let { if (it > 0) it else null }
        val currStreak = prefs.getInt("curr_streak", 0)
        val maxStreak = prefs.getInt("max_streak", 0)
        return SolitaireStats(played, won, bestTime, bestMoves, currStreak, maxStreak)
    }

    fun recordGameFinished(won: Boolean, timeSeconds: Int, moves: Int): SolitaireStats {
        val current = getStats()
        val newPlayed = current.played + 1
        val newWon = if (won) current.won + 1 else current.won
        val newBestTime = if (won) {
            if (current.bestTimeSeconds == null || timeSeconds < current.bestTimeSeconds) timeSeconds else current.bestTimeSeconds
        } else current.bestTimeSeconds
        val newBestMoves = if (won) {
            if (current.bestMoves == null || moves < current.bestMoves) moves else current.bestMoves
        } else current.bestMoves
        val newCurrStreak = if (won) current.currentStreak + 1 else 0
        val newMaxStreak = maxOf(current.maxStreak, newCurrStreak)

        prefs.edit().apply {
            putInt("played", newPlayed)
            putInt("won", newWon)
            if (newBestTime != null) putInt("best_time", newBestTime)
            if (newBestMoves != null) putInt("best_moves", newBestMoves)
            putInt("curr_streak", newCurrStreak)
            putInt("max_streak", newMaxStreak)
            apply()
        }
        return SolitaireStats(newPlayed, newWon, newBestTime, newBestMoves, newCurrStreak, newMaxStreak)
    }

    fun resetStats() {
        prefs.edit().clear().apply()
    }
}
