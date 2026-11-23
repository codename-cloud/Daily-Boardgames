package com.example.freecell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.AppSettingsStore
import com.example.home.GameDestination
import com.example.minesweeper.audio.SoundManager
import com.example.minesweeper.data.DailyStreak
import com.example.minesweeper.haptic.HapticManager
import com.example.minesweeper.model.GameStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FreeCellUiState(
    val game: FreeCellGameState = FreeCellEngine.newDeal(1),
    val status: GameStatus = GameStatus.READY,
    val timerSeconds: Int = 0,
    val selection: FreeCellSelection? = null,
    val hint: FreeCellHint? = null,
    val canUndo: Boolean = false,
    val isAutoFoundationEnabled: Boolean = true,
    val isOledMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val dailyStreak: DailyStreak = DailyStreak(),
    val stats: FreeCellStats = FreeCellStats(),
    val isNewBestTime: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showDailyStreakDialog: Boolean = false,
    val showVictoryDialog: Boolean = false
)

class FreeCellViewModel(application: Application) : AndroidViewModel(application) {
    private val statsRepository = FreeCellStatsRepository(application)
    private val soundManager = SoundManager()
    private val hapticManager = HapticManager(application)

    private val undoStack = mutableListOf<FreeCellGameState>()
    private var initialDeal: FreeCellGameState = FreeCellEngine.newDeal()
    private var timerJob: Job? = null
    private var autoFoundationJob: Job? = null
    private var currentGameRecorded = false

    private val _uiState = MutableStateFlow(FreeCellUiState())
    val uiState: StateFlow<FreeCellUiState> = _uiState.asStateFlow()

    init {
        AppSettingsStore.init(application)
        refreshSharedSettings()
        startNewGame()
    }

    fun refreshSharedSettings() {
        AppSettingsStore.reloadFromPrefs()
        val settings = AppSettingsStore.settings.value
        val streak = AppSettingsStore.getDailyStreak(GameDestination.FREECELL)
        _uiState.update {
            it.copy(
                isOledMode = settings.isOledMode,
                isSoundEnabled = settings.isSoundEnabled,
                isHapticsEnabled = settings.isHapticsEnabled,
                dailyStreak = streak,
                stats = statsRepository.getStats()
            )
        }
    }

    fun startNewGame() {
        if (!currentGameRecorded && _uiState.value.status == GameStatus.RUNNING) {
            statsRepository.recordGameFinished(
                won = false,
                timeSeconds = _uiState.value.timerSeconds,
                moves = _uiState.value.game.moves
            )
        }
        timerJob?.cancel()
        autoFoundationJob?.cancel()
        undoStack.clear()
        currentGameRecorded = false
        initialDeal = FreeCellEngine.newDeal()

        _uiState.update {
            it.copy(
                game = initialDeal,
                status = GameStatus.READY,
                timerSeconds = 0,
                selection = null,
                hint = null,
                canUndo = false,
                isNewBestTime = false,
                showVictoryDialog = false,
                stats = statsRepository.getStats()
            )
        }
    }

    fun restartCurrentDeal() {
        timerJob?.cancel()
        autoFoundationJob?.cancel()
        undoStack.clear()
        currentGameRecorded = false
        _uiState.update {
            it.copy(
                game = initialDeal,
                status = GameStatus.READY,
                timerSeconds = 0,
                selection = null,
                hint = null,
                canUndo = false,
                isNewBestTime = false,
                showVictoryDialog = false
            )
        }
    }

    fun toggleAutoFoundation() {
        _uiState.update { it.copy(isAutoFoundationEnabled = !it.isAutoFoundationEnabled) }
        if (_uiState.value.isAutoFoundationEnabled && _uiState.value.status == GameStatus.RUNNING) {
            scheduleAutoFoundations()
        }
    }

    fun onFreeCellClick(cellIdx: Int) {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        val sel = state.selection

        if (sel != null) {
            if (sel is FreeCellSelection.FreeCell && sel.index == cellIdx) {
                val smart = FreeCellEngine.smartMove(state.game, sel)
                if (smart != null) {
                    applyMove(smart)
                } else {
                    _uiState.update { it.copy(selection = null, hint = null) }
                }
                return
            }
            val next = when (sel) {
                is FreeCellSelection.FreeCell -> FreeCellEngine.moveFreeCellToFreeCell(state.game, sel.index, cellIdx)
                is FreeCellSelection.Tableau -> {
                    val pile = state.game.tableau.getOrNull(sel.col)
                    if (pile != null && sel.cardIndex == pile.lastIndex) {
                        FreeCellEngine.moveTableauToFreeCell(state.game, sel.col, cellIdx)
                    } else null
                }
                is FreeCellSelection.Foundation -> null
            }
            if (next != null) {
                applyMove(next)
                return
            }
        }

        if (state.game.freeCells.getOrNull(cellIdx) != null) {
            if (state.isHapticsEnabled) hapticManager.performRevealHaptic()
            _uiState.update { it.copy(selection = FreeCellSelection.FreeCell(cellIdx), hint = null) }
        } else {
            _uiState.update { it.copy(selection = null, hint = null) }
        }
    }

    fun onFoundationClick(foundationIdx: Int) {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        val sel = state.selection

        if (sel != null) {
            val next = when (sel) {
                is FreeCellSelection.FreeCell -> FreeCellEngine.moveFreeCellToFoundation(state.game, sel.index, foundationIdx)
                is FreeCellSelection.Tableau -> {
                    val pile = state.game.tableau.getOrNull(sel.col)
                    if (pile != null && sel.cardIndex == pile.lastIndex) {
                        FreeCellEngine.moveTableauToFoundation(state.game, sel.col, foundationIdx)
                    } else null
                }
                is FreeCellSelection.Foundation -> null
            }
            if (next != null) {
                applyMove(next)
                return
            }
        }

        if (state.game.foundations.getOrNull(foundationIdx)?.isNotEmpty() == true) {
            val newSel = if (sel == FreeCellSelection.Foundation(foundationIdx)) null else FreeCellSelection.Foundation(foundationIdx)
            _uiState.update { it.copy(selection = newSel, hint = null) }
        } else {
            _uiState.update { it.copy(selection = null, hint = null) }
        }
    }

    fun onTableauCardClick(col: Int, cardIndex: Int) {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        val pile = state.game.tableau.getOrNull(col) ?: return
        val sel = state.selection

        if (sel != null) {
            if (sel is FreeCellSelection.Tableau && sel.col == col && sel.cardIndex == cardIndex) {
                val smart = FreeCellEngine.smartMove(state.game, sel)
                if (smart != null) {
                    applyMove(smart)
                } else {
                    _uiState.update { it.copy(selection = null, hint = null) }
                }
                return
            }
            val next = when (sel) {
                is FreeCellSelection.FreeCell -> FreeCellEngine.moveFreeCellToTableau(state.game, sel.index, col)
                is FreeCellSelection.Tableau -> FreeCellEngine.moveTableauToTableau(state.game, sel.col, sel.cardIndex, col)
                is FreeCellSelection.Foundation -> FreeCellEngine.moveFoundationToTableau(state.game, sel.index, col)
            }
            if (next != null) {
                applyMove(next)
                return
            }
        }

        if (cardIndex in pile.indices && FreeCellEngine.isOrderedRun(pile, cardIndex)) {
            if (state.isHapticsEnabled) hapticManager.performRevealHaptic()
            _uiState.update { it.copy(selection = FreeCellSelection.Tableau(col, cardIndex), hint = null) }
        } else {
            _uiState.update { it.copy(selection = null, hint = null) }
        }
    }

    fun undo() {
        if (_uiState.value.status == GameStatus.WON) return
        autoFoundationJob?.cancel()
        val prev = undoStack.removeLastOrNull() ?: return
        if (_uiState.value.isSoundEnabled) soundManager.playClick()
        _uiState.update {
            it.copy(
                game = prev,
                selection = null,
                hint = null,
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    fun requestHint() {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        _uiState.update { it.copy(hint = FreeCellEngine.findHint(state.game), selection = null) }
    }

    fun toggleOledMode() {
        AppSettingsStore.toggleOledMode(getApplication())
        _uiState.update { it.copy(isOledMode = AppSettingsStore.settings.value.isOledMode) }
    }

    fun toggleSound() {
        AppSettingsStore.toggleSound(getApplication())
        _uiState.update { it.copy(isSoundEnabled = AppSettingsStore.settings.value.isSoundEnabled) }
    }

    fun showStatsDialog(show: Boolean) {
        _uiState.update { it.copy(showStatsDialog = show, stats = statsRepository.getStats()) }
    }

    fun showDailyStreakDialog(show: Boolean) {
        _uiState.update { it.copy(showDailyStreakDialog = show) }
    }

    fun dismissVictoryDialog() {
        _uiState.update { it.copy(showVictoryDialog = false) }
    }

    fun resetStats() {
        statsRepository.resetStats()
        _uiState.update { it.copy(stats = statsRepository.getStats()) }
    }

    private fun applyMove(next: FreeCellGameState, recordUndo: Boolean = true) {
        val currentState = _uiState.value
        if (recordUndo) {
            undoStack.add(currentState.game)
        }
        if (currentState.status == GameStatus.READY) {
            startTimer()
        }
        if (currentState.isSoundEnabled) soundManager.playFlag()
        if (currentState.isHapticsEnabled) hapticManager.performRevealHaptic()

        if (next.isWon) {
            handleWin(next)
        } else {
            _uiState.update {
                it.copy(
                    game = next,
                    status = GameStatus.RUNNING,
                    selection = null,
                    hint = null,
                    canUndo = undoStack.isNotEmpty()
                )
            }
            if (currentState.isAutoFoundationEnabled) {
                scheduleAutoFoundations()
            }
        }
    }

    private fun scheduleAutoFoundations() {
        autoFoundationJob?.cancel()
        autoFoundationJob = viewModelScope.launch {
            while (isActive) {
                delay(90)
                val current = _uiState.value
                if (current.status == GameStatus.WON || !current.isAutoFoundationEnabled) break
                val step = FreeCellEngine.stepSafeAutoFoundation(current.game) ?: break
                if (current.isSoundEnabled) soundManager.playClick()
                if (step.isWon) {
                    handleWin(step)
                    break
                } else {
                    _uiState.update { it.copy(game = step) }
                }
            }
        }
    }

    private fun handleWin(wonState: FreeCellGameState) {
        timerJob?.cancel()
        autoFoundationJob?.cancel()
        currentGameRecorded = true
        val time = _uiState.value.timerSeconds
        val updatedStats = statsRepository.recordGameFinished(
            won = true,
            timeSeconds = time,
            moves = wonState.moves
        )
        val isNewBest = updatedStats.bestTimeSeconds == time
        val isFirstWinToday = AppSettingsStore.recordDailyWin(getApplication(), GameDestination.FREECELL)
        val streak = AppSettingsStore.getDailyStreak(GameDestination.FREECELL)

        if (_uiState.value.isSoundEnabled) {
            soundManager.playVictory(viewModelScope)
        }

        _uiState.update {
            it.copy(
                game = wonState,
                status = GameStatus.WON,
                selection = null,
                hint = null,
                canUndo = false,
                stats = updatedStats,
                isNewBestTime = isNewBest,
                dailyStreak = streak,
                showDailyStreakDialog = isFirstWinToday,
                showVictoryDialog = !isFirstWinToday
            )
        }
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
        autoFoundationJob?.cancel()
        soundManager.release()
    }
}
