package com.example

import com.example.minesweeper.haptic.HapticStrength
import com.example.minesweeper.model.Cell
import com.example.minesweeper.model.Difficulty
import com.example.minesweeper.model.DifficultyType
import com.example.minesweeper.model.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesweeperGameTest {

    @Test
    fun testBeginnerDifficultyConfig() {
        val beginner = Difficulty.BEGINNER
        assertEquals(DifficultyType.BEGINNER, beginner.type)
        assertEquals(16, beginner.rows)
        assertEquals(9, beginner.cols)
        assertEquals(18, beginner.mines)
    }

    @Test
    fun testIntermediateDifficultyConfig() {
        val intermediate = Difficulty.INTERMEDIATE
        assertEquals(DifficultyType.INTERMEDIATE, intermediate.type)
        assertEquals(21, intermediate.rows)
        assertEquals(12, intermediate.cols)
        assertEquals(40, intermediate.mines)
    }

    @Test
    fun testExpertDifficultyConfig() {
        val expert = Difficulty.EXPERT_PORTRAIT
        assertEquals(DifficultyType.EXPERT, expert.type)
        assertEquals(30, expert.rows)
        assertEquals(16, expert.cols)
        assertEquals(99, expert.mines)
    }

    @Test
    fun testCellDefaultState() {
        val cell = Cell(row = 0, col = 0)
        assertFalse(cell.isMine)
        assertFalse(cell.isRevealed)
        assertFalse(cell.isFlagged)
        assertEquals(0, cell.adjacentMines)
        assertTrue(cell.canInteract)
    }

    @Test
    fun testHapticStrengths() {
        val values = HapticStrength.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(HapticStrength.LIGHT))
        assertTrue(values.contains(HapticStrength.MEDIUM))
        assertTrue(values.contains(HapticStrength.STRONG))
    }

    @Test
    fun testBoardOrientationVariants() {
        assertEquals(16, Difficulty.BEGINNER_PORTRAIT.rows)
        assertEquals(9, Difficulty.BEGINNER_PORTRAIT.cols)
        assertEquals(9, Difficulty.BEGINNER_LANDSCAPE.rows)
        assertEquals(16, Difficulty.BEGINNER_LANDSCAPE.cols)

        assertEquals(21, Difficulty.INTERMEDIATE_PORTRAIT.rows)
        assertEquals(12, Difficulty.INTERMEDIATE_PORTRAIT.cols)
        assertEquals(12, Difficulty.INTERMEDIATE_LANDSCAPE.rows)
        assertEquals(21, Difficulty.INTERMEDIATE_LANDSCAPE.cols)

        assertEquals(30, Difficulty.EXPERT_PORTRAIT.rows)
        assertEquals(16, Difficulty.EXPERT_PORTRAIT.cols)
        assertEquals(16, Difficulty.EXPERT_LANDSCAPE.rows)
        assertEquals(30, Difficulty.EXPERT_LANDSCAPE.cols)
    }

    @Test
    fun testDefaultOledModeState() {
        val defaultState = com.example.minesweeper.viewmodel.MinesweeperUiState()
        assertFalse(defaultState.isOledMode)
    }

    @Test
    fun testDefaultDailyStreakState() {
        val defaultState = com.example.minesweeper.viewmodel.MinesweeperUiState()
        assertEquals(0, defaultState.dailyStreak.currentStreak)
        assertEquals(0, defaultState.dailyStreak.longestStreak)
        assertFalse(defaultState.dailyStreak.isWinAwardedToday)
        assertFalse(defaultState.showDailyStreakDialog)
    }

    @Test
    fun testGuessFreeBoardGeneratorSafeStart() {
        val difficulty = Difficulty.BEGINNER
        val safeRow = 3
        val safeCol = 3
        val board = com.example.minesweeper.logic.GuessFreeBoardGenerator.generate(safeRow, safeCol, difficulty)

        assertEquals(difficulty.rows, board.size)
        assertEquals(difficulty.cols, board[0].size)

        // Count total mines
        val totalMines = board.sumOf { row -> row.count { it.isMine } }
        assertEquals(difficulty.mines, totalMines)

        // Verify 3x3 area around first click has 0 mines
        for (dr in -1..1) {
            for (dc in -1..1) {
                val r = safeRow + dr
                val c = safeCol + dc
                if (r in 0 until difficulty.rows && c in 0 until difficulty.cols) {
                    assertFalse("Cell ($r, $c) around first click must not be a mine", board[r][c].isMine)
                }
            }
        }
    }

    @Test
    fun testGuessFreeSolverKnownLayout() {
        // Simple 3x3 layout with 1 mine at (0, 0)
        val mineGrid = Array(3) { BooleanArray(3) }
        mineGrid[0][0] = true

        val solver = com.example.minesweeper.logic.GuessFreeSolver(
            rows = 3,
            cols = 3,
            totalMines = 1,
            mineGrid = mineGrid
        )
        // Starting at (2, 2) should logically deduce and solve all safe cells
        assertTrue(solver.isSolvable(2, 2))
    }

    @Test
    fun testDailyStreakDialogCopyBeforeAndAfterDailyWin() {
        val noStreak = com.example.minesweeper.data.DailyStreak(
            currentStreak = 0,
            longestStreak = 3,
            isWinAwardedToday = false
        )
        val noStreakCopy = com.example.minesweeper.ui.components.resolveDailyStreakDialogCopy(noStreak)
        assertEquals("Start a Streak!", noStreakCopy.title)
        assertTrue(noStreakCopy.subtitle.contains("Win a board today"))

        val pendingOneDay = com.example.minesweeper.data.DailyStreak(
            currentStreak = 1,
            longestStreak = 3,
            isWinAwardedToday = false
        )
        val pendingOneDayCopy = com.example.minesweeper.ui.components.resolveDailyStreakDialogCopy(pendingOneDay)
        assertEquals("1-Day Streak!", pendingOneDayCopy.title)
        assertEquals("You've won a board 1 day in a row! Win a board today to keep the flame alive!", pendingOneDayCopy.subtitle)

        val pendingMultiDay = com.example.minesweeper.data.DailyStreak(
            currentStreak = 5,
            longestStreak = 5,
            isWinAwardedToday = false
        )
        val pendingMultiDayCopy = com.example.minesweeper.ui.components.resolveDailyStreakDialogCopy(pendingMultiDay)
        assertEquals("5-Day Streak!", pendingMultiDayCopy.title)
        assertEquals("You've won a board 5 days in a row! Win a board today to keep the flame alive!", pendingMultiDayCopy.subtitle)

        val completedFirstDay = com.example.minesweeper.data.DailyStreak(
            currentStreak = 1,
            longestStreak = 1,
            isWinAwardedToday = true
        )
        val completedFirstDayCopy = com.example.minesweeper.ui.components.resolveDailyStreakDialogCopy(completedFirstDay)
        assertEquals("First Win of the Day!", completedFirstDayCopy.title)
        assertTrue(completedFirstDayCopy.subtitle.contains("completed your first board today"))

        val completedMultiDay = com.example.minesweeper.data.DailyStreak(
            currentStreak = 5,
            longestStreak = 5,
            isWinAwardedToday = true
        )
        val completedMultiDayCopy = com.example.minesweeper.ui.components.resolveDailyStreakDialogCopy(completedMultiDay)
        assertEquals("5-Day Streak!", completedMultiDayCopy.title)
        assertTrue(completedMultiDayCopy.subtitle.contains("Come back tomorrow to keep the flame alive"))
    }
}
