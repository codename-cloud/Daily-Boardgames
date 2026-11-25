package com.example.common

import android.content.Context
import android.content.SharedPreferences
import com.example.home.GameDestination
import com.example.minesweeper.data.DailyStreak
import com.example.minesweeper.data.DailyStreakManager
import com.example.minesweeper.haptic.HapticStrength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettings(
    val isOledMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isFlagHapticsEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val seenTutorials: Set<GameDestination> = emptySet()
)

object AppSettingsStore {
    private const val PREFS_NAME = "minesweeper_settings_prefs"

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _dailyStreaks = MutableStateFlow<Map<GameDestination, DailyStreak>>(emptyMap())
    val dailyStreaks: StateFlow<Map<GameDestination, DailyStreak>> = _dailyStreaks.asStateFlow()

    @Volatile
    private var initialized = false
    private lateinit var prefs: SharedPreferences
    private lateinit var streakManagers: Map<GameDestination, DailyStreakManager>

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            streakManagers = mapOf(
                GameDestination.MINESWEEPER to DailyStreakManager(appContext, "minesweeper_daily_streak_prefs"),
                GameDestination.SOLITAIRE to DailyStreakManager(appContext, "solitaire_daily_streak_prefs"),
                GameDestination.FREECELL to DailyStreakManager(appContext, "freecell_daily_streak_prefs"),
                GameDestination.SUDOKU to DailyStreakManager(appContext, "sudoku_daily_streak_prefs")
            )
            reloadFromPrefs()
            initialized = true
        }
    }

    fun reloadFromPrefs() {
        if (!::prefs.isInitialized) return
        val strength = try {
            HapticStrength.valueOf(
                prefs.getString("haptic_strength", HapticStrength.MEDIUM.name) ?: HapticStrength.MEDIUM.name
            )
        } catch (_: Exception) {
            HapticStrength.MEDIUM
        }
        val seen = GameDestination.entries.filter { dest ->
            prefs.getBoolean(tutorialPrefKey(dest), false)
        }.toSet()
        _settings.value = AppSettings(
            isOledMode = prefs.getBoolean("oled_mode", false),
            isSoundEnabled = prefs.getBoolean("sound_enabled", true),
            isHapticsEnabled = prefs.getBoolean("haptics_enabled", true),
            isFlagHapticsEnabled = prefs.getBoolean("flag_haptics_enabled", true),
            hapticStrength = strength,
            seenTutorials = seen
        )
        _dailyStreaks.value = streakManagers.mapValues { (_, manager) -> manager.getStreak() }
    }

    private fun tutorialPrefKey(game: GameDestination): String =
        "tutorial_seen_${game.name.lowercase()}"

    fun setOledMode(context: Context, enabled: Boolean) {
        init(context)
        prefs.edit().putBoolean("oled_mode", enabled).apply()
        _settings.update { it.copy(isOledMode = enabled) }
    }

    fun toggleOledMode(context: Context) {
        init(context)
        setOledMode(context, !_settings.value.isOledMode)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        init(context)
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _settings.update { it.copy(isSoundEnabled = enabled) }
    }

    fun toggleSound(context: Context) {
        init(context)
        setSoundEnabled(context, !_settings.value.isSoundEnabled)
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        init(context)
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _settings.update { it.copy(isHapticsEnabled = enabled) }
    }

    fun setFlagHapticsEnabled(context: Context, enabled: Boolean) {
        init(context)
        prefs.edit().putBoolean("flag_haptics_enabled", enabled).apply()
        _settings.update { it.copy(isFlagHapticsEnabled = enabled) }
    }

    fun setHapticStrength(context: Context, strength: HapticStrength) {
        init(context)
        prefs.edit().putString("haptic_strength", strength.name).apply()
        _settings.update { it.copy(hapticStrength = strength) }
    }

    fun getDailyStreak(game: GameDestination): DailyStreak {
        return _dailyStreaks.value[game] ?: DailyStreak()
    }

    fun refreshStreak(context: Context, game: GameDestination): DailyStreak {
        init(context)
        val streak = streakManagers.getValue(game).getStreak()
        _dailyStreaks.update { current -> current + (game to streak) }
        return streak
    }

    fun recordDailyWin(context: Context, game: GameDestination): Boolean {
        init(context)
        val res = streakManagers.getValue(game).recordWin()
        _dailyStreaks.update { current -> current + (game to res.streak) }
        return res.isFirstWinToday
    }

    fun hasSeenTutorial(game: GameDestination): Boolean {
        return _settings.value.seenTutorials.contains(game)
    }

    fun markTutorialSeen(context: Context, game: GameDestination) {
        init(context)
        prefs.edit().putBoolean(tutorialPrefKey(game), true).apply()
        _settings.update { it.copy(seenTutorials = it.seenTutorials + game) }
    }

    internal fun resetForTesting() {
        initialized = false
        _settings.value = AppSettings()
        _dailyStreaks.value = emptyMap()
    }
}
