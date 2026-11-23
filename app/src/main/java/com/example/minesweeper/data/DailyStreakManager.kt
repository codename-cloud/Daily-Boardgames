package com.example.minesweeper.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyStreak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastWinDate: String? = null,
    val isWinAwardedToday: Boolean = false
)

data class StreakWinResult(
    val streak: DailyStreak,
    val isFirstWinToday: Boolean
)

class DailyStreakManager(
    context: Context,
    prefsName: String = "minesweeper_daily_streak_prefs"
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayString(): String = dateFormat.format(Date())

    fun getYesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    fun getStreak(): DailyStreak {
        val today = getTodayString()
        val yesterday = getYesterdayString()
        val lastWinDate = prefs.getString("daily_streak_last_win_date", null)
        var currentStreak = prefs.getInt("daily_streak_count", 0)
        val longestStreak = prefs.getInt("daily_streak_longest", 0)

        val isWinAwardedToday = (lastWinDate == today)

        // The streak restarts whenever a day is missed
        if (lastWinDate != null && lastWinDate != today && lastWinDate != yesterday) {
            currentStreak = 0
            prefs.edit().putInt("daily_streak_count", 0).apply()
        }

        return DailyStreak(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastWinDate = lastWinDate,
            isWinAwardedToday = isWinAwardedToday
        )
    }

    /**
     * Records a game win. If this is the first won board of the day, increments or starts
     * the streak and returns isFirstWinToday = true.
     */
    fun recordWin(): StreakWinResult {
        val today = getTodayString()
        val yesterday = getYesterdayString()
        val currentStreakData = getStreak()

        if (currentStreakData.lastWinDate == today) {
            // Board already won today; streak is preserved and not re-incremented
            return StreakWinResult(currentStreakData, isFirstWinToday = false)
        }

        // First win of today: extend streak if played yesterday, else start at 1
        val newStreak = if (currentStreakData.lastWinDate == yesterday) {
            currentStreakData.currentStreak + 1
        } else {
            1
        }

        val newLongest = maxOf(currentStreakData.longestStreak, newStreak)

        prefs.edit().apply {
            putInt("daily_streak_count", newStreak)
            putInt("daily_streak_longest", newLongest)
            putString("daily_streak_last_win_date", today)
            apply()
        }

        val updatedStreak = DailyStreak(
            currentStreak = newStreak,
            longestStreak = newLongest,
            lastWinDate = today,
            isWinAwardedToday = true
        )

        return StreakWinResult(updatedStreak, isFirstWinToday = true)
    }

    fun resetStreak() {
        prefs.edit().clear().apply()
    }
}
