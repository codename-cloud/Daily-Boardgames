package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.common.AppSettingsStore
import com.example.home.GameDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerGameDailyStreakTest {

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

        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.SUDOKU).currentStreak)
        assertFalse(AppSettingsStore.getDailyStreak(GameDestination.SUDOKU).isWinAwardedToday)

        // Win a Solitaire game and verify only Solitaire increments
        val isFirstSolitaire = AppSettingsStore.recordDailyWin(fakeContext, GameDestination.SOLITAIRE)
        assertTrue(isFirstSolitaire)
        assertEquals(1, AppSettingsStore.getDailyStreak(GameDestination.SOLITAIRE).currentStreak)
        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.FREECELL).currentStreak)
        assertEquals(0, AppSettingsStore.getDailyStreak(GameDestination.SUDOKU).currentStreak)
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
