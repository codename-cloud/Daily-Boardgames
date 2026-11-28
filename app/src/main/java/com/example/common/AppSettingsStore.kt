package com.example.common

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.home.GameDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class Handedness(val label: String) {
    LEFT_HANDED("Left-Handed"),
    RIGHT_HANDED("Right-Handed")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isOledMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isFlagHapticsEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val seenTutorials: Set<GameDestination> = emptySet(),
    val dailyWinsGames: Set<GameDestination> = GameDestination.entries.toSet(),
    val solitaireHandedness: Handedness = Handedness.LEFT_HANDED,
    val spiderHandedness: Handedness = Handedness.RIGHT_HANDED,
    val githubRepo: String = ""
) {
    fun resolveIsDark(systemDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun resolveIsOled(systemDark: Boolean): Boolean = resolveIsDark(systemDark) && isOledMode
}

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
                GameDestination.SPIDER_SOLITAIRE to DailyStreakManager(appContext, "spider_daily_streak_prefs"),
                GameDestination.SUDOKU to DailyStreakManager(appContext, "sudoku_daily_streak_prefs")
            )
            reloadFromPrefs()
            initialized = true
        }
    }

    fun reloadFromPrefs() {
        if (!::prefs.isInitialized) return
        val themeMode = try {
            ThemeMode.valueOf(
                prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            )
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
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
        val trackedDailyGames = GameDestination.entries.filter { dest ->
            prefs.getBoolean(dailyWinsPrefKey(dest), true)
        }.toSet().ifEmpty { GameDestination.entries.toSet() }
        val defaultRepo = normalizeGithubRepo(BuildConfig.DEFAULT_GITHUB_REPO).orEmpty()
        val storedRepo = prefs.getString("github_repo", null).orEmpty()
        val effectiveRepo = defaultRepo.ifEmpty { storedRepo }
        val storedOled = prefs.getBoolean("oled_mode", false)
        val solitaireHandedness = try {
            Handedness.valueOf(
                prefs.getString("solitaire_handedness", Handedness.LEFT_HANDED.name)
                    ?: Handedness.LEFT_HANDED.name
            )
        } catch (_: Exception) {
            Handedness.LEFT_HANDED
        }
        val spiderHandedness = try {
            Handedness.valueOf(
                prefs.getString("spider_handedness", Handedness.RIGHT_HANDED.name)
                    ?: Handedness.RIGHT_HANDED.name
            )
        } catch (_: Exception) {
            Handedness.RIGHT_HANDED
        }
        _settings.value = AppSettings(
            themeMode = themeMode,
            isOledMode = if (themeMode == ThemeMode.LIGHT) false else storedOled,
            isSoundEnabled = prefs.getBoolean("sound_enabled", true),
            isHapticsEnabled = prefs.getBoolean("haptics_enabled", true),
            isFlagHapticsEnabled = prefs.getBoolean("flag_haptics_enabled", true),
            hapticStrength = strength,
            seenTutorials = seen,
            dailyWinsGames = trackedDailyGames,
            solitaireHandedness = solitaireHandedness,
            spiderHandedness = spiderHandedness,
            githubRepo = effectiveRepo
        )
        _dailyStreaks.value = streakManagers.mapValues { (_, manager) -> manager.getStreak() }
    }

    private fun tutorialPrefKey(game: GameDestination): String =
        "tutorial_seen_${game.name.lowercase()}"

    private fun dailyWinsPrefKey(game: GameDestination): String =
        "daily_wins_include_${game.name.lowercase()}"

    fun setGithubRepo(context: Context, repoInput: String): String {
        init(context)
        val normalized = normalizeGithubRepo(repoInput).orEmpty()
        prefs.edit().putString("github_repo", normalized).apply()
        _settings.update { it.copy(githubRepo = normalized) }
        return normalized
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        init(context)
        val mode = if (enabled) ThemeMode.DARK else ThemeMode.LIGHT
        if (!enabled) {
            prefs.edit()
                .putString("theme_mode", mode.name)
                .putBoolean("oled_mode", false)
                .apply()
            _settings.update { it.copy(themeMode = mode, isOledMode = false) }
        } else {
            prefs.edit().putString("theme_mode", mode.name).apply()
            _settings.update { it.copy(themeMode = mode) }
        }
    }

    fun setOledMode(context: Context, enabled: Boolean) {
        init(context)
        if (enabled && _settings.value.themeMode == ThemeMode.LIGHT) {
            return
        }
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

    fun setGameCountsForDailyWins(context: Context, game: GameDestination, enabled: Boolean) {
        init(context)
        val current = _settings.value.dailyWinsGames
        if (!enabled && current.size <= 1 && game in current) {
            return
        }
        val updated = if (enabled) current + game else current - game
        prefs.edit().putBoolean(dailyWinsPrefKey(game), enabled).apply()
        _settings.update { it.copy(dailyWinsGames = updated) }
    }

    fun setSolitaireHandedness(context: Context, handedness: Handedness) {
        init(context)
        prefs.edit().putString("solitaire_handedness", handedness.name).apply()
        _settings.update { it.copy(solitaireHandedness = handedness) }
    }

    fun setSpiderHandedness(context: Context, handedness: Handedness) {
        init(context)
        prefs.edit().putString("spider_handedness", handedness.name).apply()
        _settings.update { it.copy(spiderHandedness = handedness) }
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

    private const val KEY_DAILY_SWEEP_CELEBRATED_DATE = "daily_sweep_celebrated_date"

    fun consumeDailySweepCelebrationIfReady(
        context: Context,
        trackedGames: Set<GameDestination> = _settings.value.dailyWinsGames
    ): Boolean {
        init(context)
        val targetGames = trackedGames.ifEmpty { return false }
        val completedToday = targetGames.count { game ->
            _dailyStreaks.value[game]?.isWinAwardedToday == true
        }
        if (completedToday < targetGames.size) return false
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastCelebrated = prefs.getString(KEY_DAILY_SWEEP_CELEBRATED_DATE, null)
        if (lastCelebrated == today) return false
        prefs.edit().putString(KEY_DAILY_SWEEP_CELEBRATED_DATE, today).apply()
        return true
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
