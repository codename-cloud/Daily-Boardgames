package com.example.sudoku

import android.content.Context
import android.content.SharedPreferences

class SudokuStatsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sudoku_stats_prefs", Context.MODE_PRIVATE)

    fun getStats(difficulty: SudokuDifficulty): SudokuStats {
        val played = prefs.getInt("${difficulty.name}_played", 0)
        val won = prefs.getInt("${difficulty.name}_won", 0)
        val bestTime = prefs.getInt("${difficulty.name}_best_time", -1).let { if (it > 0) it else null }
        val currStreak = prefs.getInt("${difficulty.name}_curr_streak", 0)
        val maxStreak = prefs.getInt("${difficulty.name}_max_streak", 0)
        return SudokuStats(played, won, bestTime, currStreak, maxStreak)
    }

    fun recordGameFinished(difficulty: SudokuDifficulty, won: Boolean, timeSeconds: Int): SudokuStats {
        val current = getStats(difficulty)
        val newPlayed = current.played + 1
        val newWon = if (won) current.won + 1 else current.won
        val newBestTime = if (won) {
            if (current.bestTimeSeconds == null || timeSeconds < current.bestTimeSeconds) timeSeconds else current.bestTimeSeconds
        } else current.bestTimeSeconds
        val newCurrStreak = if (won) current.currentStreak + 1 else 0
        val newMaxStreak = maxOf(current.maxStreak, newCurrStreak)

        prefs.edit().apply {
            putInt("${difficulty.name}_played", newPlayed)
            putInt("${difficulty.name}_won", newWon)
            if (newBestTime != null) putInt("${difficulty.name}_best_time", newBestTime)
            putInt("${difficulty.name}_curr_streak", newCurrStreak)
            putInt("${difficulty.name}_max_streak", newMaxStreak)
            apply()
        }
        return SudokuStats(newPlayed, newWon, newBestTime, newCurrStreak, newMaxStreak)
    }

    fun resetStats(difficulty: SudokuDifficulty) {
        prefs.edit().apply {
            remove("${difficulty.name}_played")
            remove("${difficulty.name}_won")
            remove("${difficulty.name}_best_time")
            remove("${difficulty.name}_curr_streak")
            remove("${difficulty.name}_max_streak")
            apply()
        }
    }
}
