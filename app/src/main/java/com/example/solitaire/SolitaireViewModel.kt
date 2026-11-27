package com.example.solitaire

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

data class SolitaireUiState(
    val game: SolitaireGameState = SolitaireEngine.newDeal(),
    val dealGeneration: Int = 0,
    val status: GameStatus = GameStatus.READY,
    val timerSeconds: Int = 0,
    val selection: SolitaireSelection? = null,
    val canUndo: Boolean = false,
    val canAutoComplete: Boolean = false,
    val isAutoCompleting: Boolean = false,
    val isOledMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val dailyStreak: DailyStreak = DailyStreak(),
    val stats: SolitaireStats = SolitaireStats(),
    val isNewBestTime: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showDailyStreakDialog: Boolean = false,
    val showVictoryDialog: Boolean = false
)

class SolitaireViewModel(application: Application) : AndroidViewModel(application) {
    private val statsRepository = SolitaireStatsRepository(application)
    private val soundManager = SoundManager()
    private val hapticManager = HapticManager(application)

    private val undoStack = mutableListOf<SolitaireGameState>()
    private var initialDeal: SolitaireGameState = SolitaireEngine.newDeal()
    private var timerJob: Job? = null
    private var autoCompleteJob: Job? = null
    private var currentGameRecorded = false

    private val _uiState = MutableStateFlow(SolitaireUiState())
    val uiState: StateFlow<SolitaireUiState> = _uiState.asStateFlow()

    init {
        AppSettingsStore.init(application)
        refreshSharedSettings()
        startNewGame(SolitaireDrawMode.DRAW_1)
    }

    fun refreshSharedSettings() {
        AppSettingsStore.reloadFromPrefs()
        val settings = AppSettingsStore.settings.value
        val streak = AppSettingsStore.getDailyStreak(GameDestination.SOLITAIRE)
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

    fun startNewGame(drawMode: SolitaireDrawMode = _uiState.value.game.drawMode) {
        if (currentGameRecorded.not() && _uiState.value.status == GameStatus.RUNNING) {
            statsRepository.recordGameFinished(
                won = false,
                timeSeconds = _uiState.value.timerSeconds,
                moves = _uiState.value.game.moves
            )
        }
        timerJob?.cancel()
        autoCompleteJob?.cancel()
        undoStack.clear()
        currentGameRecorded = false
        initialDeal = SolitaireEngine.newDeal(drawMode = drawMode)

        _uiState.update {
            it.copy(
                game = initialDeal,
                dealGeneration = it.dealGeneration + 1,
                status = GameStatus.READY,
                timerSeconds = 0,
                selection = null,
                canUndo = false,
                canAutoComplete = false,
                isAutoCompleting = false,
                isNewBestTime = false,
                showVictoryDialog = false,
                stats = statsRepository.getStats()
            )
        }
    }

    fun restartCurrentDeal() {
        timerJob?.cancel()
        autoCompleteJob?.cancel()
        undoStack.clear()
        currentGameRecorded = false
        _uiState.update {
            it.copy(
                game = initialDeal,
                dealGeneration = it.dealGeneration + 1,
                status = GameStatus.READY,
                timerSeconds = 0,
                selection = null,
                canUndo = false,
                canAutoComplete = false,
                isAutoCompleting = false,
                isNewBestTime = false,
                showVictoryDialog = false
            )
        }
    }

    fun setDrawMode(mode: SolitaireDrawMode) {
        if (mode == _uiState.value.game.drawMode) return
        startNewGame(mode)
    }

    fun onStockClick() {
        if (_uiState.value.status == GameStatus.WON || _uiState.value.isAutoCompleting) return
        val next = SolitaireEngine.drawFromStock(_uiState.value.game) ?: return
        applyMove(next, isCardPlace = false)
    }

    fun onWasteClick() {
        val state = _uiState.value
        if (state.status == GameStatus.WON || state.isAutoCompleting) return
        if (state.game.waste.isEmpty()) return

        if (state.selection == SolitaireSelection.Waste) {
            // Second tap auto-moves waste card
            val moved = SolitaireEngine.smartMove(state.game, SolitaireSelection.Waste)
            if (moved != null) {
                applyMove(moved, isCardPlace = true)
            } else {
                _uiState.update { it.copy(selection = null) }
            }
        } else {
            if (state.isHapticsEnabled) hapticManager.performRevealHaptic()
            _uiState.update { it.copy(selection = SolitaireSelection.Waste) }
        }
    }

    fun onFoundationClick(foundationIdx: Int) {
        val state = _uiState.value
        if (state.status == GameStatus.WON || state.isAutoCompleting) return
        val sel = state.selection

        if (sel != null) {
            val next = when (sel) {
                is SolitaireSelection.Waste -> SolitaireEngine.moveWasteToFoundation(state.game, foundationIdx)
                is SolitaireSelection.Tableau -> SolitaireEngine.moveTableauToFoundation(state.game, sel.col, foundationIdx)
                is SolitaireSelection.Foundation -> null
            }
            if (next != null) {
                applyMove(next, isCardPlace = true)
                return
            }
        }

        // Select foundation top card if present
        if (state.game.foundations.getOrNull(foundationIdx)?.isNotEmpty() == true) {
            val newSel = if (sel == SolitaireSelection.Foundation(foundationIdx)) null else SolitaireSelection.Foundation(foundationIdx)
            _uiState.update { it.copy(selection = newSel) }
        } else {
            _uiState.update { it.copy(selection = null) }
        }
    }

    fun onTableauCardClick(col: Int, cardIndex: Int) {
        val state = _uiState.value
        if (state.status == GameStatus.WON || state.isAutoCompleting) return
        val pile = state.game.tableau.getOrNull(col) ?: return

        val sel = state.selection
        if (sel != null) {
            // Tapping the same selected card triggers smart auto-move
            if (sel is SolitaireSelection.Tableau && sel.col == col && sel.cardIndex == cardIndex) {
                val smart = SolitaireEngine.smartMove(state.game, sel)
                if (smart != null) {
                    applyMove(smart, isCardPlace = true)
                } else {
                    _uiState.update { it.copy(selection = null) }
                }
                return
            }

            // Try moving current selection onto this tableau column
            val next = when (sel) {
                is SolitaireSelection.Waste -> SolitaireEngine.moveWasteToTableau(state.game, col)
                is SolitaireSelection.Tableau -> SolitaireEngine.moveTableauToTableau(state.game, sel.col, sel.cardIndex, col)
                is SolitaireSelection.Foundation -> SolitaireEngine.moveFoundationToTableau(state.game, sel.index, col)
            }
            if (next != null) {
                applyMove(next, isCardPlace = true)
                return
            }
        }

        // Otherwise select the tapped face-up card (or stack starting at cardIndex)
        val tappedCard = pile.getOrNull(cardIndex)
        if (tappedCard != null && tappedCard.isFaceUp) {
            if (state.isHapticsEnabled) hapticManager.performRevealHaptic()
            _uiState.update { it.copy(selection = SolitaireSelection.Tableau(col, cardIndex)) }
        } else {
            _uiState.update { it.copy(selection = null) }
        }
    }

    fun undo() {
        if (_uiState.value.isAutoCompleting || _uiState.value.status == GameStatus.WON) return
        val previous = undoStack.removeLastOrNull() ?: return
        if (_uiState.value.isSoundEnabled) soundManager.playClick()
        _uiState.update {
            it.copy(
                game = previous,
                selection = null,
                canUndo = undoStack.isNotEmpty(),
                canAutoComplete = SolitaireEngine.canAutoComplete(previous)
            )
        }
    }

    fun triggerAutoComplete() {
        val state = _uiState.value
        if (!state.canAutoComplete || state.isAutoCompleting) return
        _uiState.update { it.copy(isAutoCompleting = true, selection = null) }
        autoCompleteJob?.cancel()
        autoCompleteJob = viewModelScope.launch {
            while (isActive) {
                val current = _uiState.value.game
                val next = SolitaireEngine.stepAutoComplete(current) ?: break
                applyMove(next, isCardPlace = true, recordUndo = false, performHaptic = false)
                if (next.isWon) break
                delay(90)
            }
            _uiState.update { it.copy(isAutoCompleting = false) }
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
        _uiState.update {
            it.copy(showStatsDialog = show, stats = statsRepository.getStats())
        }
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

    private fun applyMove(
        next: SolitaireGameState,
        isCardPlace: Boolean,
        recordUndo: Boolean = true,
        performHaptic: Boolean = true
    ) {
        val currentState = _uiState.value
        if (recordUndo) {
            undoStack.add(currentState.game)
        }
        if (currentState.status == GameStatus.READY) {
            startTimer()
        }

        if (currentState.isSoundEnabled) {
            if (isCardPlace) soundManager.playFlag() else soundManager.playClick()
        }
        if (performHaptic && currentState.isHapticsEnabled && isCardPlace) {
            hapticManager.performRevealHaptic()
        }

        if (next.isWon) {
            timerJob?.cancel()
            currentGameRecorded = true
            val time = _uiState.value.timerSeconds
            val updatedStats = statsRepository.recordGameFinished(
                won = true,
                timeSeconds = time,
                moves = next.moves
            )
            val isNewBest = updatedStats.bestTimeSeconds == time
            val isFirstWinToday = AppSettingsStore.recordDailyWin(getApplication(), GameDestination.SOLITAIRE)
            val streak = AppSettingsStore.getDailyStreak(GameDestination.SOLITAIRE)

            if (currentState.isSoundEnabled) {
                soundManager.playVictory(viewModelScope)
            }

            _uiState.update {
                it.copy(
                    game = next,
                    status = GameStatus.WON,
                    selection = null,
                    canUndo = false,
                    canAutoComplete = false,
                    isAutoCompleting = false,
                    stats = updatedStats,
                    isNewBestTime = isNewBest,
                    dailyStreak = streak,
                    showDailyStreakDialog = isFirstWinToday,
                    showVictoryDialog = !isFirstWinToday
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    game = next,
                    status = GameStatus.RUNNING,
                    selection = null,
                    canUndo = undoStack.isNotEmpty(),
                    canAutoComplete = SolitaireEngine.canAutoComplete(next)
                )
            }
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
        autoCompleteJob?.cancel()
        soundManager.release()
    }
}
