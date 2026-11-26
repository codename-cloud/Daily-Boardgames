package com.example.sudoku

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.AppSettingsStore
import com.example.home.GameDestination
import com.example.minesweeper.audio.SoundManager
import com.example.minesweeper.data.DailyStreak
import com.example.minesweeper.haptic.HapticManager
import com.example.minesweeper.model.GameStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SudokuUiState(
    val difficulty: SudokuDifficulty = SudokuDifficulty.EASY,
    val board: List<List<SudokuCell>> = emptyList(),
    val status: GameStatus = GameStatus.READY,
    val timerSeconds: Int = 0,
    val selectedCell: Pair<Int, Int>? = null,
    val isNotesMode: Boolean = false,
    val mistakes: Int = 0,
    val resetCount: Int = 0,
    val remainingEmptyCells: Int = 41,
    val canUndo: Boolean = false,
    val isOledMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val dailyStreak: DailyStreak = DailyStreak(),
    val stats: Map<SudokuDifficulty, SudokuStats> = emptyMap(),
    val isNewBestTime: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showDailyStreakDialog: Boolean = false,
    val showVictoryDialog: Boolean = false
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {
    private val statsRepository = SudokuStatsRepository(application)
    private val soundManager = SoundManager()
    private val hapticManager = HapticManager(application)

    private val undoStack = mutableListOf<List<List<SudokuCell>>>()
    private var timerJob: Job? = null
    private var currentGameRecorded = false

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    init {
        AppSettingsStore.init(application)
        refreshSharedSettings()
        startNewGame(SudokuDifficulty.EASY)
    }

    fun refreshSharedSettings() {
        AppSettingsStore.reloadFromPrefs()
        val settings = AppSettingsStore.settings.value
        val streak = AppSettingsStore.getDailyStreak(GameDestination.SUDOKU)
        _uiState.update {
            it.copy(
                isOledMode = settings.isOledMode,
                isSoundEnabled = settings.isSoundEnabled,
                isHapticsEnabled = settings.isHapticsEnabled,
                dailyStreak = streak,
                stats = loadAllStats()
            )
        }
    }

    private fun loadAllStats(): Map<SudokuDifficulty, SudokuStats> {
        return SudokuDifficulty.entries.associateWith { statsRepository.getStats(it) }
    }

    fun startNewGame(difficulty: SudokuDifficulty = _uiState.value.difficulty) {
        if (!currentGameRecorded && _uiState.value.status == GameStatus.RUNNING) {
            statsRepository.recordGameFinished(_uiState.value.difficulty, won = false, timeSeconds = _uiState.value.timerSeconds)
        }
        timerJob?.cancel()
        undoStack.clear()
        currentGameRecorded = false

        viewModelScope.launch {
            val board = withContext(Dispatchers.Default) {
                SudokuGenerator.generatePuzzle(difficulty)
            }
            val emptyCount = board.sumOf { row -> row.count { it.value == 0 } }
            _uiState.update {
                it.copy(
                    difficulty = difficulty,
                    board = board,
                    status = GameStatus.READY,
                    timerSeconds = 0,
                    selectedCell = null,
                    mistakes = 0,
                    remainingEmptyCells = emptyCount,
                    canUndo = false,
                    isNewBestTime = false,
                    showVictoryDialog = false,
                    stats = loadAllStats()
                )
            }
        }
    }

    fun selectCell(row: Int, col: Int) {
        if (_uiState.value.status == GameStatus.WON) return
        if (_uiState.value.isHapticsEnabled) hapticManager.performRevealHaptic()
        _uiState.update { it.copy(selectedCell = row to col) }
    }

    fun toggleNotesMode() {
        _uiState.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun enterDigit(digit: Int) {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        val (r, c) = state.selectedCell ?: return
        val cell = state.board.getOrNull(r)?.getOrNull(c) ?: return
        if (cell.isGiven) return

        if (state.status == GameStatus.READY) {
            startTimer()
        }

        undoStack.add(state.board)

        if (state.isNotesMode) {
            if (cell.value != 0) return
            val newNotes = if (digit in cell.notes) cell.notes - digit else cell.notes + digit
            val updatedBoard = state.board.mapIndexed { rowIdx, rowList ->
                rowList.mapIndexed { colIdx, cur ->
                    if (rowIdx == r && colIdx == c) cur.copy(notes = newNotes) else cur
                }
            }
            if (state.isSoundEnabled) soundManager.playClick()
            _uiState.update {
                it.copy(
                    board = updatedBoard,
                    status = GameStatus.RUNNING,
                    canUndo = undoStack.isNotEmpty()
                )
            }
            return
        }

        // Normal digit entry
        val isMistake = digit != cell.solution
        val newValue = if (cell.value == digit) 0 else digit
        val isError = newValue != 0 && newValue != cell.solution

        var updatedBoard = state.board.mapIndexed { rowIdx, rowList ->
            rowList.mapIndexed { colIdx, cur ->
                if (rowIdx == r && colIdx == c) {
                    cur.copy(
                        value = newValue,
                        notes = if (newValue == 0) cur.notes else emptySet(),
                        isError = isError
                    )
                } else {
                    cur
                }
            }
        }

        // Auto-clear matching note in row, col, and 3x3 box when placing a valid digit
        if (newValue != 0 && !isError) {
            val boxR = (r / 3) * 3
            val boxC = (c / 3) * 3
            updatedBoard = updatedBoard.mapIndexed { rowIdx, rowList ->
                rowList.mapIndexed { colIdx, cur ->
                    val samePeer = rowIdx == r || colIdx == c ||
                        (rowIdx in boxR until boxR + 3 && colIdx in boxC until boxC + 3)
                    if (samePeer && newValue in cur.notes) {
                        cur.copy(notes = cur.notes - newValue)
                    } else {
                        cur
                    }
                }
            }
        }

        if (isError) {
            if (state.isSoundEnabled) soundManager.playExplosion()
            if (state.isHapticsEnabled) hapticManager.performFlagHaptic()
        } else {
            if (state.isSoundEnabled) soundManager.playFlag()
            if (state.isHapticsEnabled) hapticManager.performRevealHaptic()
        }

        val newMistakes = if (newValue != 0 && isMistake) state.mistakes + 1 else state.mistakes
        if (newMistakes >= 3) {
            resetCurrentBoardAfterMistakeLimit()
            return
        }
        applyBoardUpdate(updatedBoard, newMistakes)
    }

    fun eraseCell() {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        val (r, c) = state.selectedCell ?: return
        val cell = state.board.getOrNull(r)?.getOrNull(c) ?: return
        if (cell.isGiven || (cell.value == 0 && cell.notes.isEmpty())) return

        undoStack.add(state.board)
        val updatedBoard = state.board.mapIndexed { rowIdx, rowList ->
            rowList.mapIndexed { colIdx, cur ->
                if (rowIdx == r && colIdx == c) cur.copy(value = 0, notes = emptySet(), isError = false) else cur
            }
        }
        if (state.isSoundEnabled) soundManager.playClick()
        applyBoardUpdate(updatedBoard, state.mistakes)
    }

    private fun resetCurrentBoardAfterMistakeLimit() {
        timerJob?.cancel()
        undoStack.clear()
        val clearedBoard = _uiState.value.board.map { rowList ->
            rowList.map { cell ->
                if (cell.isGiven) cell
                else cell.copy(value = 0, notes = emptySet(), isError = false)
            }
        }
        val emptyCount = clearedBoard.sumOf { row -> row.count { it.value == 0 } }
        _uiState.update {
            it.copy(
                board = clearedBoard,
                status = GameStatus.READY,
                timerSeconds = 0,
                selectedCell = null,
                mistakes = 0,
                resetCount = it.resetCount + 1,
                remainingEmptyCells = emptyCount,
                canUndo = false
            )
        }
    }

    fun undo() {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        val prev = undoStack.removeLastOrNull() ?: return
        if (state.isSoundEnabled) soundManager.playClick()
        applyBoardUpdate(prev, state.mistakes)
    }

    private fun applyBoardUpdate(board: List<List<SudokuCell>>, mistakes: Int) {
        val remainingToSolve = board.sumOf { row -> row.count { it.value != it.solution } }
        val isWon = board.isNotEmpty() && remainingToSolve == 0

        if (isWon) {
            timerJob?.cancel()
            currentGameRecorded = true
            val time = _uiState.value.timerSeconds
            val updatedStats = statsRepository.recordGameFinished(
                difficulty = _uiState.value.difficulty,
                won = true,
                timeSeconds = time
            )
            val isNewBest = updatedStats.bestTimeSeconds == time
            val isFirstWinToday = AppSettingsStore.recordDailyWin(getApplication(), GameDestination.SUDOKU)
            val streak = AppSettingsStore.getDailyStreak(GameDestination.SUDOKU)

            if (_uiState.value.isSoundEnabled) {
                soundManager.playVictory(viewModelScope)
            }

            _uiState.update {
                it.copy(
                    board = board,
                    mistakes = mistakes,
                    remainingEmptyCells = 0,
                    status = GameStatus.WON,
                    canUndo = false,
                    isNewBestTime = isNewBest,
                    dailyStreak = streak,
                    stats = loadAllStats(),
                    showDailyStreakDialog = isFirstWinToday,
                    showVictoryDialog = !isFirstWinToday
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    board = board,
                    mistakes = mistakes,
                    remainingEmptyCells = board.sumOf { row -> row.count { c -> c.value == 0 || c.isError } },
                    status = GameStatus.RUNNING,
                    canUndo = undoStack.isNotEmpty()
                )
            }
        }
    }

    fun setOledMode(enabled: Boolean) {
        AppSettingsStore.setOledMode(getApplication(), enabled)
        _uiState.update { it.copy(isOledMode = enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        AppSettingsStore.setSoundEnabled(getApplication(), enabled)
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        AppSettingsStore.setHapticsEnabled(getApplication(), enabled)
        _uiState.update { it.copy(isHapticsEnabled = enabled) }
    }

    fun showSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun showStatsDialog(show: Boolean) {
        _uiState.update { it.copy(showStatsDialog = show, stats = loadAllStats()) }
    }

    fun showDailyStreakDialog(show: Boolean) {
        _uiState.update { it.copy(showDailyStreakDialog = show) }
    }

    fun dismissVictoryDialog() {
        _uiState.update { it.copy(showVictoryDialog = false) }
    }

    fun resetStats(difficulty: SudokuDifficulty) {
        statsRepository.resetStats(difficulty)
        _uiState.update { it.copy(stats = loadAllStats()) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.timerSeconds < 999) {
                delay(1000)
                _uiState.update { it.copy(timerSeconds = (it.timerSeconds + 1).coerceAtMost(999)) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundManager.release()
    }
}
