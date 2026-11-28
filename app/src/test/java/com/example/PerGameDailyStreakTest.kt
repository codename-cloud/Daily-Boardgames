package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.common.AppSettingsStore
import com.example.common.Handedness
import com.example.common.ThemeMode
import com.example.common.tutorialStepsFor
import com.example.home.GameDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PerGameDailyStreakTest {

    @Before
    fun setUp() {
        AppSettingsStore.resetForTesting()
    }

    @Test
    fun testEachGameHasIndependentDailyStreak() {
        val fakeContext = InMemoryPrefsContext()
        AppSettingsStore.init(fakeContext)

        // Win a Minesweeper board
        val isFirstMinesweeper = AppSettingsStore.recordDailyWin(fakeContext, GameDestination.MINESWEEPER)
        assertTrue(isFirstMinesweeper)
        assertEquals(1, AppSettingsStore.getDailyStreak(GameDestination.MINESWEEPER).currentStreak)
        assertTrue(AppSettingsStore.getDailyStreak(GameDestination.MINESWEEPER).isWinAwardedToday)

        // Other games' daily streaks must remain completely unaffected
        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.SOLITAIRE).currentStreak)
        assertFalse(AppSettingsStore.getDailyStreak(GameDestination.SOLITAIRE).isWinAwardedToday)

        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.FREECELL).currentStreak)
        assertFalse(AppSettingsStore.getDailyStreak(GameDestination.FREECELL).isWinAwardedToday)

        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.SPIDER_SOLITAIRE).currentStreak)
        assertFalse(AppSettingsStore.getDailyStreak(GameDestination.SPIDER_SOLITAIRE).isWinAwardedToday)

        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.SUDOKU).currentStreak)
        assertFalse(AppSettingsStore.getDailyStreak(GameDestination.SUDOKU).isWinAwardedToday)

        // Win a Solitaire game and verify only Solitaire increments
        val isFirstSolitaire = AppSettingsStore.recordDailyWin(fakeContext, GameDestination.SOLITAIRE)
        assertTrue(isFirstSolitaire)
        assertEquals(1, AppSettingsStore.getDailyStreak(GameDestination.SOLITAIRE).currentStreak)
        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.FREECELL).currentStreak)
        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.SPIDER_SOLITAIRE).currentStreak)
        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.SUDOKU).currentStreak)
    }

    @Test
    fun testTutorialSeenStateIsTrackedAndPersistedPerGame() {
        val fakeContext = InMemoryPrefsContext()
        AppSettingsStore.init(fakeContext)

        GameDestination.entries.forEach { game ->
            assertFalse("Tutorial for $game should default to unseen", AppSettingsStore.hasSeenTutorial(game))
        }

        AppSettingsStore.markTutorialSeen(fakeContext, GameDestination.MINESWEEPER)
        assertTrue(AppSettingsStore.hasSeenTutorial(GameDestination.MINESWEEPER))
        assertFalse(AppSettingsStore.hasSeenTutorial(GameDestination.SOLITAIRE))
        assertFalse(AppSettingsStore.hasSeenTutorial(GameDestination.FREECELL))
        assertFalse(AppSettingsStore.hasSeenTutorial(GameDestination.SUDOKU))

        AppSettingsStore.markTutorialSeen(fakeContext, GameDestination.SUDOKU)
        assertTrue(AppSettingsStore.hasSeenTutorial(GameDestination.SUDOKU))

        // Simulate process restart against same backing SharedPreferences
        AppSettingsStore.resetForTesting()
        AppSettingsStore.init(fakeContext)
        assertTrue(AppSettingsStore.hasSeenTutorial(GameDestination.MINESWEEPER))
        assertTrue(AppSettingsStore.hasSeenTutorial(GameDestination.SUDOKU))
        assertFalse(AppSettingsStore.hasSeenTutorial(GameDestination.SOLITAIRE))
        assertFalse(AppSettingsStore.hasSeenTutorial(GameDestination.FREECELL))
    }

    @Test
    fun testAllGamesHaveMultiStepTutorials() {
        GameDestination.entries.forEach { game ->
            val steps = tutorialStepsFor(game)
            assertTrue("Expected multi-step tutorial for $game", steps.size >= 3)
            steps.forEach { step ->
                assertTrue(step.title.isNotBlank())
                assertTrue(step.description.isNotBlank())
            }
        }
    }

    @Test
    fun testThemeModeDefaultsToSystemAndBlocksOledInLightMode() {
        val fakeContext = InMemoryPrefsContext()
        AppSettingsStore.init(fakeContext)

        // Default follows device system theme
        assertEquals(ThemeMode.SYSTEM, AppSettingsStore.settings.value.themeMode)
        assertTrue(AppSettingsStore.settings.value.resolveIsDark(systemDark = true))
        assertFalse(AppSettingsStore.settings.value.resolveIsDark(systemDark = false))

        // Switch to Dark Mode and enable OLED
        AppSettingsStore.setDarkMode(fakeContext, true)
        assertEquals(ThemeMode.DARK, AppSettingsStore.settings.value.themeMode)
        assertTrue(AppSettingsStore.settings.value.resolveIsDark(systemDark = false))

        AppSettingsStore.setOledMode(fakeContext, true)
        assertTrue(AppSettingsStore.settings.value.isOledMode)
        assertTrue(AppSettingsStore.settings.value.resolveIsOled(systemDark = false))

        // Switching to Light Mode disables OLED and blocks enabling it while Light Mode is active
        AppSettingsStore.setDarkMode(fakeContext, false)
        assertEquals(ThemeMode.LIGHT, AppSettingsStore.settings.value.themeMode)
        assertFalse(AppSettingsStore.settings.value.resolveIsDark(systemDark = true))
        assertFalse(AppSettingsStore.settings.value.isOledMode)
        assertFalse(AppSettingsStore.settings.value.resolveIsOled(systemDark = true))

        AppSettingsStore.setOledMode(fakeContext, true)
        assertFalse(AppSettingsStore.settings.value.isOledMode)

        // Persists across process restart
        AppSettingsStore.resetForTesting()
        AppSettingsStore.init(fakeContext)
        assertEquals(ThemeMode.LIGHT, AppSettingsStore.settings.value.themeMode)
        assertFalse(AppSettingsStore.settings.value.isOledMode)
    }

    @Test
    fun testDailySweepCelebrationTriggersOnceAfterAllTrackedGamesWonToday() {
        val fakeContext = InMemoryPrefsContext()
        AppSettingsStore.init(fakeContext)

        assertFalse(AppSettingsStore.consumeDailySweepCelebrationIfReady(fakeContext))

        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.MINESWEEPER)
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.SOLITAIRE)
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.FREECELL)
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.SPIDER_SOLITAIRE)
        assertFalse(AppSettingsStore.consumeDailySweepCelebrationIfReady(fakeContext))

        // 5th daily win completes 5/5
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.SUDOKU)
        assertTrue(AppSettingsStore.consumeDailySweepCelebrationIfReady(fakeContext))

        // Subsequent returns to the home screen on the same day should not re-trigger
        assertFalse(AppSettingsStore.consumeDailySweepCelebrationIfReady(fakeContext))

        // Even across process restart on the same day, the celebrated state is persisted
        AppSettingsStore.resetForTesting()
        AppSettingsStore.init(fakeContext)
        assertFalse(AppSettingsStore.consumeDailySweepCelebrationIfReady(fakeContext))
    }

    @Test
    fun testDailyWinsSelectionExcludesGamesAndPersistsAcrossRestart() {
        val fakeContext = InMemoryPrefsContext()
        AppSettingsStore.init(fakeContext)

        // All games enabled by default
        assertEquals(GameDestination.entries.toSet(), AppSettingsStore.settings.value.dailyWinsGames)

        // Exclude Sudoku from Daily Wins
        AppSettingsStore.setGameCountsForDailyWins(fakeContext, GameDestination.SUDOKU, false)
        assertFalse(GameDestination.SUDOKU in AppSettingsStore.settings.value.dailyWinsGames)
        assertEquals(4, AppSettingsStore.settings.value.dailyWinsGames.size)

        // Winning the remaining 4 games triggers Daily Sweep without winning Sudoku
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.MINESWEEPER)
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.SOLITAIRE)
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.FREECELL)
        AppSettingsStore.recordDailyWin(fakeContext, GameDestination.SPIDER_SOLITAIRE)
        assertTrue(AppSettingsStore.consumeDailySweepCelebrationIfReady(fakeContext))

        // Guard prevents disabling the final remaining game
        AppSettingsStore.setGameCountsForDailyWins(fakeContext, GameDestination.MINESWEEPER, false)
        AppSettingsStore.setGameCountsForDailyWins(fakeContext, GameDestination.SOLITAIRE, false)
        AppSettingsStore.setGameCountsForDailyWins(fakeContext, GameDestination.FREECELL, false)
        AppSettingsStore.setGameCountsForDailyWins(fakeContext, GameDestination.SPIDER_SOLITAIRE, false)
        assertEquals(setOf(GameDestination.SPIDER_SOLITAIRE), AppSettingsStore.settings.value.dailyWinsGames)

        // Persists across process restart
        AppSettingsStore.resetForTesting()
        AppSettingsStore.init(fakeContext)
        assertEquals(setOf(GameDestination.SPIDER_SOLITAIRE), AppSettingsStore.settings.value.dailyWinsGames)
    }

    @Test
    fun testSolitaireAndSpiderHandednessDefaultsAndPersistence() {
        val fakeContext = InMemoryPrefsContext()
        AppSettingsStore.init(fakeContext)

        assertEquals(Handedness.LEFT_HANDED, AppSettingsStore.settings.value.solitaireHandedness)
        assertEquals(Handedness.RIGHT_HANDED, AppSettingsStore.settings.value.spiderHandedness)

        AppSettingsStore.setSolitaireHandedness(fakeContext, Handedness.RIGHT_HANDED)
        AppSettingsStore.setSpiderHandedness(fakeContext, Handedness.LEFT_HANDED)

        assertEquals(Handedness.RIGHT_HANDED, AppSettingsStore.settings.value.solitaireHandedness)
        assertEquals(Handedness.LEFT_HANDED, AppSettingsStore.settings.value.spiderHandedness)

        AppSettingsStore.resetForTesting()
        AppSettingsStore.init(fakeContext)
        assertEquals(Handedness.RIGHT_HANDED, AppSettingsStore.settings.value.solitaireHandedness)
        assertEquals(Handedness.LEFT_HANDED, AppSettingsStore.settings.value.spiderHandedness)
    }

    private class InMemoryPrefsContext : ContextWrapper(null) {
        private val prefsMap = mutableMapOf<String, InMemorySharedPreferences>()

        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            return prefsMap.getOrPut(name) { InMemorySharedPreferences() }
        }
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { if (key != null) pending[key] = values }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { if (key != null) pending[key] = null }
            override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearAll) data.clear()
                for ((k, v) in pending) {
                    if (v == null) data.remove(k) else data[k] = v
                }
            }
        }
    }
}
