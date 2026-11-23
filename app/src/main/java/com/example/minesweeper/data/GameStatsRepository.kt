package com.example.minesweeper.data

import android.content.Context
import android.content.SharedPreferences
import com.example.minesweeper.model.DifficultyType

data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestTimeSeconds: Int? = null,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0
) {
    val winPercentage: Int
        get() = if (played > 0) (won * 100) / played else 0
}

class GameStatsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("minesweeper_stats_prefs", Context.MODE_PRIVATE)

    fun getStats(type: DifficultyType): DifficultyStats {
        val played = prefs.getInt("${type.name}_played", 0)
        val won = prefs.getInt("${type.name}_won", 0)
        val bestTime = prefs.getInt("${type.name}_best_time", -1).let { if (it > 0) it else null }
        val currentStreak = prefs.getInt("${type.name}_curr_streak", 0)
        val maxStreak = prefs.getInt("${type.name}_max_streak", 0)
        return DifficultyStats(played, won, bestTime, currentStreak, maxStreak)
    }

    fun recordGameFinished(type: DifficultyType, won: Boolean, timeSeconds: Int): DifficultyStats {
        val current = getStats(type)
        val newPlayed = current.played + 1
        val newWon = if (won) current.won + 1 else current.won
        val newBestTime = if (won) {
            if (current.bestTimeSeconds == null || timeSeconds < current.bestTimeSeconds) timeSeconds else current.bestTimeSeconds
        } else current.bestTimeSeconds
        val newCurrentStreak = if (won) current.currentStreak + 1 else 0
        val newMaxStreak = maxOf(current.maxStreak, newCurrentStreak)

        prefs.edit().apply {
            putInt("${type.name}_played", newPlayed)
            putInt("${type.name}_won", newWon)
            if (newBestTime != null) putInt("${type.name}_best_time", newBestTime)
            putInt("${type.name}_curr_streak", newCurrentStreak)
            putInt("${type.name}_max_streak", newMaxStreak)
            apply()
        }
        return DifficultyStats(newPlayed, newWon, newBestTime, newCurrentStreak, newMaxStreak)
    }

    fun resetStats(type: DifficultyType) {
        prefs.edit().apply {
            remove("${type.name}_played")
            remove("${type.name}_won")
            remove("${type.name}_best_time")
            remove("${type.name}_curr_streak")
            remove("${type.name}_max_streak")
            apply()
        }
    }
}
