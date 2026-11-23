package com.example.minesweeper.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minesweeper.audio.SoundManager
import com.example.minesweeper.data.DailyStreak
import com.example.minesweeper.data.DailyStreakManager
import com.example.minesweeper.data.DifficultyStats
import com.example.minesweeper.data.GameStatsRepository
import com.example.minesweeper.haptic.HapticManager
import com.example.minesweeper.haptic.HapticStrength
import com.example.minesweeper.logic.GuessFreeBoardGenerator
import com.example.minesweeper.model.Cell
import com.example.minesweeper.model.Difficulty
import com.example.minesweeper.model.DifficultyType
import com.example.minesweeper.model.GameStatus
import com.example.minesweeper.model.InteractionMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MinesweeperUiState(
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val status: GameStatus = GameStatus.READY,
    val interactionMode: InteractionMode = InteractionMode.DIG,
    val timerSeconds: Int = 0,
    val remainingMines: Int = Difficulty.BEGINNER.mines,
    val board: List<List<Cell>> = emptyList(),
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isFlagHapticsEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val fillScreen: Boolean = true,
    val isExpertRotated: Boolean = false,
    val canUndo: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showHapticSettingsDialog: Boolean = false,
    val showDifficultyDialog: Boolean = false,
    val isOledMode: Boolean = false,
    val dailyStreak: DailyStreak = DailyStreak(),
    val showDailyStreakDialog: Boolean = false,
    val stats: Map<DifficultyType, DifficultyStats> = emptyMap(),
    val lastWonTime: Int? = null,
    val isNewBestTime: Boolean = false
)

class MinesweeperViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("minesweeper_settings_prefs", Context.MODE_PRIVATE)
    private val statsRepository = GameStatsRepository(application)
    private val dailyStreakManager = DailyStreakManager(application)
    private val soundManager = SoundManager()
    private val hapticManager = HapticManager(application)

    private val _uiState = MutableStateFlow(
        MinesweeperUiState(
            isSoundEnabled = prefs.getBoolean("sound_enabled", true),
            isHapticsEnabled = prefs.getBoolean("haptics_enabled", true),
            isFlagHapticsEnabled = prefs.getBoolean("flag_haptics_enabled", true),
            fillScreen = prefs.getBoolean("fill_screen", true),
            isOledMode = prefs.getBoolean("oled_mode", false),
            dailyStreak = dailyStreakManager.getStreak(),
            hapticStrength = try {
                HapticStrength.valueOf(prefs.getString("haptic_strength", HapticStrength.MEDIUM.name) ?: HapticStrength.MEDIUM.name)
            } catch (_: Exception) {
                HapticStrength.MEDIUM
            }
        )
    )
    val uiState: StateFlow<MinesweeperUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var undoSnapshot: List<List<Cell>>? = null

    init {
        com.example.common.AppSettingsStore.init(application)
        loadStats()
        refreshDailyStreak()
        startNewGame(Difficulty.BEGINNER)
    }

    fun refreshSettingsAndStreak() {
        com.example.common.AppSettingsStore.reloadFromPrefs()
        val appSettings = com.example.common.AppSettingsStore.settings.value
        val streak = com.example.common.AppSettingsStore.getDailyStreak(com.example.home.GameDestination.MINESWEEPER)
        _uiState.update {
            it.copy(
                isOledMode = appSettings.isOledMode,
                isSoundEnabled = appSettings.isSoundEnabled,
                isHapticsEnabled = appSettings.isHapticsEnabled,
                isFlagHapticsEnabled = appSettings.isFlagHapticsEnabled,
                hapticStrength = appSettings.hapticStrength,
                dailyStreak = streak
            )
        }
    }

    fun refreshDailyStreak() {
        val streak = com.example.common.AppSettingsStore.refreshStreak(
            getApplication(),
            com.example.home.GameDestination.MINESWEEPER
        )
        _uiState.update { it.copy(dailyStreak = streak) }
    }

    private fun loadStats() {
        val statsMap = DifficultyType.values().associateWith { statsRepository.getStats(it) }
        _uiState.update { it.copy(stats = statsMap) }
    }

    fun startNewGame(difficulty: Difficulty = _uiState.value.difficulty) {
        timerJob?.cancel()
        timerJob = null
        undoSnapshot = null

        val emptyBoard = List(difficulty.rows) { r ->
            List(difficulty.cols) { c ->
                Cell(row = r, col = c)
            }
        }

        _uiState.update {
            it.copy(
                difficulty = difficulty,
                status = GameStatus.READY,
                timerSeconds = 0,
                remainingMines = difficulty.mines,
                board = emptyBoard,
                canUndo = false,
                isNewBestTime = false,
                lastWonTime = null
            )
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        startNewGame(difficulty)
    }

    fun toggleOrientation() {
        val currentState = _uiState.value
        val current = currentState.difficulty
        if (current.rows == current.cols) return

        val isToLandscape = current.rows > current.cols
        val newDifficulty = when (current.type) {
            DifficultyType.BEGINNER -> if (isToLandscape) Difficulty.BEGINNER_LANDSCAPE else Difficulty.BEGINNER_PORTRAIT
            DifficultyType.INTERMEDIATE -> if (isToLandscape) Difficulty.INTERMEDIATE_LANDSCAPE else Difficulty.INTERMEDIATE_PORTRAIT
            DifficultyType.EXPERT -> if (isToLandscape) Difficulty.EXPERT_LANDSCAPE else Difficulty.EXPERT_PORTRAIT
            else -> current.copy(
                rows = current.cols,
                cols = current.rows,
                description = "${current.cols}x${current.rows} • ${current.mines} Mines"
            )
        }

        val oldBoard = currentState.board
        val rotatedBoard = if (oldBoard.isNotEmpty()) {
            rotateBoard(oldBoard, isToLandscape)
        } else {
            List(newDifficulty.rows) { r ->
                List(newDifficulty.cols) { c ->
                    Cell(row = r, col = c)
                }
            }
        }

        undoSnapshot = undoSnapshot?.let { rotateBoard(it, isToLandscape) }

        _uiState.update {
            it.copy(
                difficulty = newDifficulty,
                board = rotatedBoard,
                isExpertRotated = !isToLandscape
            )
        }
    }

    private fun rotateBoard(
        oldBoard: List<List<Cell>>,
        isToLandscape: Boolean
    ): List<List<Cell>> {
        val oldRows = oldBoard.size
        if (oldRows == 0) return emptyList()
        val oldCols = oldBoard[0].size
        val newRows = oldCols
        val newCols = oldRows

        val newGrid = MutableList(newRows) { MutableList<Cell?>(newCols) { null } }

        for (r in 0 until oldRows) {
            for (c in 0 until oldCols) {
                val cell = oldBoard[r][c]
                val newR: Int
                val newC: Int
                if (isToLandscape) {
                    newR = c
                    newC = oldRows - 1 - r
                } else {
                    newR = oldCols - 1 - c
                    newC = r
                }
                newGrid[newR][newC] = cell.copy(row = newR, col = newC)
            }
        }

        return newGrid.map { row -> row.filterNotNull() }
    }

    fun toggleExpertOrientation() {
        toggleOrientation()
    }

    fun setBoardOrientation(toLandscape: Boolean) {
        val current = _uiState.value.difficulty
        if (current.rows == current.cols) return
        val currentlyLandscape = current.rows < current.cols
        if (currentlyLandscape == toLandscape) return
        toggleOrientation()
    }

    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(interactionMode = mode) }
    }

    fun toggleInteractionMode() {
        _uiState.update {
            val nextMode = if (it.interactionMode == InteractionMode.DIG) InteractionMode.FLAG else InteractionMode.DIG
            it.copy(interactionMode = nextMode)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        com.example.common.AppSettingsStore.setSoundEnabled(getApplication(), enabled)
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun toggleSound() { setSoundEnabled(!_uiState.value.isSoundEnabled) }

    fun setHapticsEnabled(enabled: Boolean) {
        com.example.common.AppSettingsStore.setHapticsEnabled(getApplication(), enabled)
        _uiState.update { it.copy(isHapticsEnabled = enabled) }
    }

    fun toggleHaptics() { setHapticsEnabled(!_uiState.value.isHapticsEnabled) }

    fun setFlagHapticsEnabled(enabled: Boolean) {
        com.example.common.AppSettingsStore.setFlagHapticsEnabled(getApplication(), enabled)
        _uiState.update { it.copy(isFlagHapticsEnabled = enabled) }
    }

    fun setHapticStrength(strength: HapticStrength) {
        com.example.common.AppSettingsStore.setHapticStrength(getApplication(), strength)
        _uiState.update { it.copy(hapticStrength = strength) }
    }

    fun testFlagHaptic() {
        hapticManager.performFlagHaptic(_uiState.value.hapticStrength)
    }

    fun setFillScreen(fill: Boolean) {
        prefs.edit().putBoolean("fill_screen", fill).apply()
        _uiState.update { it.copy(fillScreen = fill) }
    }

    fun toggleFillScreen() {
        setFillScreen(!_uiState.value.fillScreen)
    }

    fun setOledMode(enabled: Boolean) {
        com.example.common.AppSettingsStore.setOledMode(getApplication(), enabled)
        _uiState.update { it.copy(isOledMode = enabled) }
    }

    fun toggleOledMode() {
        setOledMode(!_uiState.value.isOledMode)
    }

    fun showStatsDialog(show: Boolean) {
        if (show) loadStats()
        _uiState.update { it.copy(showStatsDialog = show) }
    }

    fun showHapticSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showHapticSettingsDialog = show) }
    }

    fun showDailyStreakDialog(show: Boolean) {
        _uiState.update { it.copy(showDailyStreakDialog = show) }
    }

    fun resetStats(type: DifficultyType) {
        statsRepository.resetStats(type)
        loadStats()
    }

    fun onCellPrimaryAction(row: Int, col: Int) {
        val currentState = _uiState.value
        if (currentState.status == GameStatus.WON || currentState.status == GameStatus.LOST) return

        val cell = currentState.board.getOrNull(row)?.getOrNull(col) ?: return
        if (cell.isRevealed) {
            if (cell.adjacentMines > 0) {
                onCellChord(row, col)
            }
            return
        }

        when (currentState.interactionMode) {
            InteractionMode.DIG -> revealCell(row, col)
            InteractionMode.FLAG -> toggleFlag(row, col)
        }
    }

    fun onCellSecondaryAction(row: Int, col: Int) {
        val currentState = _uiState.value
        if (currentState.status == GameStatus.WON || currentState.status == GameStatus.LOST) return

        val cell = currentState.board.getOrNull(row)?.getOrNull(col) ?: return
        if (cell.isRevealed) {
            if (cell.adjacentMines > 0) {
                onCellChord(row, col)
            }
            return
        }

        when (currentState.interactionMode) {
            InteractionMode.DIG -> toggleFlag(row, col)
            InteractionMode.FLAG -> revealCell(row, col)
        }
    }

    fun onCellChord(row: Int, col: Int) {
        val currentState = _uiState.value
        if (currentState.status != GameStatus.RUNNING) return
        val board = currentState.board
        val cell = board.getOrNull(row)?.getOrNull(col) ?: return

        if (!cell.isRevealed || cell.adjacentMines == 0) return

        val neighbors = getNeighbors(row, col, board)
        val flaggedNeighbors = neighbors.count { it.isFlagged }

        if (flaggedNeighbors >= cell.adjacentMines) {
            val unrevealedUnflagged = neighbors.filter { !it.isRevealed && !it.isFlagged }
            if (unrevealedUnflagged.isEmpty()) return

            undoSnapshot = board.map { r -> r.map { it.copy() } }

            if (currentState.isSoundEnabled) soundManager.playChord()
            if (currentState.isHapticsEnabled) hapticManager.performRevealHaptic()

            var currentBoard = board
            var hitMine = false
            var trippedCell: Cell? = null

            for (neighbor in unrevealedUnflagged) {
                if (neighbor.isMine) {
                    hitMine = true
                    trippedCell = neighbor
                    break
                }
            }

            if (hitMine && trippedCell != null) {
                handleGameOver(trippedCell.row, trippedCell.col)
            } else {
                for (neighbor in unrevealedUnflagged) {
                    currentBoard = cascadeReveal(neighbor.row, neighbor.col, currentBoard)
                }
                updateBoardAndCheckWin(currentBoard)
            }
        }
    }

    private fun revealCell(row: Int, col: Int) {
        val currentState = _uiState.value
        var board = currentState.board
        val targetCell = board.getOrNull(row)?.getOrNull(col) ?: return

        if (targetCell.isRevealed || targetCell.isFlagged) return

        if (currentState.status == GameStatus.READY) {
            board = initializeMines(row, col, currentState.difficulty)
            startTimer()
            _uiState.update { it.copy(status = GameStatus.RUNNING) }
        }

        val clickedCell = board[row][col]
        if (clickedCell.isMine) {
            handleGameOver(row, col)
            return
        }

        if (currentState.isSoundEnabled) soundManager.playClick()
        if (currentState.isHapticsEnabled) hapticManager.performRevealHaptic()

        undoSnapshot = board.map { r -> r.map { it.copy() } }
        val newBoard = cascadeReveal(row, col, board)
        updateBoardAndCheckWin(newBoard)
    }

    private fun toggleFlag(row: Int, col: Int) {
        val currentState = _uiState.value
        val board = currentState.board
        val targetCell = board.getOrNull(row)?.getOrNull(col) ?: return

        if (targetCell.isRevealed) return

        val newFlagged = !targetCell.isFlagged
        if (currentState.isSoundEnabled) soundManager.playFlag()
        if (currentState.isHapticsEnabled && currentState.isFlagHapticsEnabled) {
            hapticManager.performFlagHaptic(currentState.hapticStrength)
        }

        val newBoard = board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(isFlagged = newFlagged) else cell
            }
        }

        val flagsCount = newBoard.sumOf { rowCells -> rowCells.count { it.isFlagged } }
        val remaining = currentState.difficulty.mines - flagsCount

        _uiState.update {
            it.copy(board = newBoard, remainingMines = remaining)
        }
    }

    private fun cascadeReveal(startRow: Int, startCol: Int, currentBoard: List<List<Cell>>): List<List<Cell>> {
        val mutableBoard = currentBoard.map { it.toMutableList() }.toMutableList()
        val rows = mutableBoard.size
        val cols = mutableBoard[0].size
        val queue = ArrayDeque<Pair<Int, Int>>()

        val start = mutableBoard[startRow][startCol]
        if (start.isRevealed || start.isFlagged) return currentBoard

        mutableBoard[startRow][startCol] = start.copy(isRevealed = true)
        if (start.adjacentMines == 0) {
            queue.add(startRow to startCol)
        }

        val directions = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1,          0 to 1,
            1 to -1,  1 to 0,  1 to 1
        )

        while (queue.isNotEmpty()) {
            val (currRow, currCol) = queue.removeFirst()

            for ((dr, dc) in directions) {
                val nr = currRow + dr
                val nc = currCol + dc

                if (nr in 0 until rows && nc in 0 until cols) {
                    val neighbor = mutableBoard[nr][nc]
                    if (!neighbor.isRevealed && !neighbor.isFlagged && !neighbor.isMine) {
                        mutableBoard[nr][nc] = neighbor.copy(isRevealed = true)
                        if (neighbor.adjacentMines == 0) {
                            queue.add(nr to nc)
                        }
                    }
                }
            }
        }

        return mutableBoard.map { it.toList() }
    }

    private fun updateBoardAndCheckWin(board: List<List<Cell>>) {
        val difficulty = _uiState.value.difficulty
        val totalCells = difficulty.rows * difficulty.cols
        val totalSafeCells = totalCells - difficulty.mines

        val revealedCount = board.sumOf { rowCells -> rowCells.count { it.isRevealed && !it.isMine } }

        if (revealedCount >= totalSafeCells) {
            timerJob?.cancel()
            val time = _uiState.value.timerSeconds

            if (_uiState.value.isSoundEnabled) {
                soundManager.playVictory(viewModelScope)
            }

            val winBoard = board.map { rowList ->
                rowList.map { cell ->
                    if (cell.isMine) cell.copy(isFlagged = true) else cell
                }
            }

            val updatedStats = statsRepository.recordGameFinished(
                type = difficulty.type,
                won = true,
                timeSeconds = time
            )

            val isNewBest = updatedStats.bestTimeSeconds == time
            val isFirstWinToday = com.example.common.AppSettingsStore.recordDailyWin(
                getApplication(),
                com.example.home.GameDestination.MINESWEEPER
            )
            val streak = com.example.common.AppSettingsStore.getDailyStreak(com.example.home.GameDestination.MINESWEEPER)

            _uiState.update {
                it.copy(
                    board = winBoard,
                    status = GameStatus.WON,
                    remainingMines = 0,
                    canUndo = false,
                    lastWonTime = time,
                    isNewBestTime = isNewBest,
                    dailyStreak = streak,
                    showDailyStreakDialog = isFirstWinToday
                )
            }
            loadStats()
        } else {
            val flagsCount = board.sumOf { rowCells -> rowCells.count { it.isFlagged } }
            val remaining = difficulty.mines - flagsCount
            _uiState.update {
                it.copy(
                    board = board,
                    remainingMines = remaining,
                    canUndo = true
                )
            }
        }
    }

    private fun handleGameOver(explodedRow: Int, explodedCol: Int) {
        timerJob?.cancel()
        val currentState = _uiState.value

        if (currentState.isSoundEnabled) soundManager.playExplosion()
        if (currentState.isHapticsEnabled) hapticManager.performExplosionHaptic()

        statsRepository.recordGameFinished(
            type = currentState.difficulty.type,
            won = false,
            timeSeconds = currentState.timerSeconds
        )
        loadStats()

        val revealedBoard = currentState.board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, cell ->
                when {
                    r == explodedRow && c == explodedCol -> cell.copy(isRevealed = true, isExploded = true)
                    cell.isMine && !cell.isFlagged -> cell.copy(isRevealed = true)
                    !cell.isMine && cell.isFlagged -> cell.copy(isFalseFlag = true)
                    else -> cell
                }
            }
        }

        _uiState.update {
            it.copy(
                board = revealedBoard,
                status = GameStatus.LOST,
                canUndo = undoSnapshot != null
            )
        }
    }

    fun undoLastMove() {
        val snapshot = undoSnapshot ?: return
        if (_uiState.value.status != GameStatus.LOST) return

        val flagsCount = snapshot.sumOf { rowCells -> rowCells.count { it.isFlagged } }
        val remaining = _uiState.value.difficulty.mines - flagsCount

        _uiState.update {
            it.copy(board = snapshot, status = GameStatus.RUNNING, remainingMines = remaining, canUndo = false)
        }
        undoSnapshot = null
        startTimer()
    }

    private fun initializeMines(safeRow: Int, safeCol: Int, difficulty: Difficulty): List<List<Cell>> {
        return GuessFreeBoardGenerator.generate(
            safeRow = safeRow,
            safeCol = safeCol,
            difficulty = difficulty,
            existingBoard = _uiState.value.board
        )
    }

    private fun getNeighbors(row: Int, col: Int, board: List<List<Cell>>): List<Cell> {
        val rows = board.size
        val cols = board[0].size
        val result = mutableListOf<Cell>()

        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = row + dr
                val nc = col + dc
                if (nr in 0 until rows && nc in 0 until cols) {
                    result.add(board[nr][nc])
                }
            }
        }
        return result
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
        soundManager.release()
        timerJob?.cancel()
    }
}
