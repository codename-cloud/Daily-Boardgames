package com.example.spider

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.AppSettingsStore
import com.example.common.DailyStreak
import com.example.common.GameStatus
import com.example.common.Handedness
import com.example.common.HapticManager
import com.example.common.SoundManager
import com.example.home.GameDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SpiderUiState(
    val game: SpiderGameState = SpiderGameState(),
    val dealGeneration: Int = 0,
    val status: GameStatus = GameStatus.READY,
    val timerSeconds: Int = 0,
    val selection: SpiderSelection? = null,
    val canUndo: Boolean = false,
    val stockBlockedMessage: String? = null,
    val isOledMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val handedness: Handedness = Handedness.RIGHT_HANDED,
    val dailyStreak: DailyStreak = DailyStreak(),
    val stats: SpiderStats = SpiderStats(),
    val isNewBestTime: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showDailyStreakDialog: Boolean = false,
    val showVictoryDialog: Boolean = false
)

class SpiderViewModel(application: Application) : AndroidViewModel(application) {
    private val statsRepository = SpiderStatsRepository(application)
    private val soundManager = SoundManager()
    private val hapticManager = HapticManager(application)

    private val undoStack = mutableListOf<SpiderGameState>()
    private var initialDeal: SpiderGameState = SpiderGameState()
    private var timerJob: Job? = null
    private var noticeJob: Job? = null
    private var currentGameRecorded = false

    private val _uiState = MutableStateFlow(SpiderUiState())
    val uiState: StateFlow<SpiderUiState> = _uiState.asStateFlow()

    init {
        AppSettingsStore.init(application)
        refreshSharedSettings()
        startNewGame(SpiderSuitMode.ONE_SUIT)
    }

    fun refreshSharedSettings() {
        AppSettingsStore.reloadFromPrefs()
        val settings = AppSettingsStore.settings.value
        val streak = AppSettingsStore.getDailyStreak(GameDestination.SPIDER_SOLITAIRE)
        _uiState.update {
            it.copy(
                isOledMode = settings.isOledMode,
                isSoundEnabled = settings.isSoundEnabled,
                isHapticsEnabled = settings.isHapticsEnabled,
                handedness = settings.spiderHandedness,
                dailyStreak = streak,
                stats = statsRepository.getStats()
            )
        }
    }

    fun startNewGame(suitMode: SpiderSuitMode = _uiState.value.game.suitMode) {
        if (!currentGameRecorded && _uiState.value.status == GameStatus.RUNNING) {
            statsRepository.recordGameFinished(
                won = false,
                timeSeconds = _uiState.value.timerSeconds,
                moves = _uiState.value.game.moves
            )
        }
        timerJob?.cancel()
        noticeJob?.cancel()
        undoStack.clear()
        currentGameRecorded = false
        initialDeal = SpiderEngine.newDeal(suitMode = suitMode)

        _uiState.update {
            it.copy(
                game = initialDeal,
                dealGeneration = it.dealGeneration + 1,
                status = GameStatus.READY,
                timerSeconds = 0,
                selection = null,
                canUndo = false,
                stockBlockedMessage = null,
                isNewBestTime = false,
                showVictoryDialog = false,
                stats = statsRepository.getStats()
            )
        }
    }

    fun restartCurrentDeal() {
        timerJob?.cancel()
        noticeJob?.cancel()
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
                stockBlockedMessage = null,
                isNewBestTime = false,
                showVictoryDialog = false
            )
        }
    }

    fun setSuitMode(mode: SpiderSuitMode) {
        if (mode == _uiState.value.game.suitMode) return
        startNewGame(mode)
    }

    fun onStockClick() {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        if (state.game.stock.isEmpty()) return

        if (SpiderEngine.isStockDealBlockedByEmptyColumn(state.game)) {
            if (state.isHapticsEnabled) hapticManager.performRevealHaptic()
            showStockBlockedNotice("Fill all empty columns before dealing from Stock")
            return
        }

        val next = SpiderEngine.dealFromStock(state.game) ?: return
        applyMove(next, isCardPlace = false)
    }

    fun onTableauCardClick(col: Int, cardIndex: Int) {
        val state = _uiState.value
        if (state.status == GameStatus.WON) return
        val pile = state.game.tableau.getOrNull(col) ?: return

        val sel = state.selection
        if (sel != null) {
            if (sel.col == col && sel.cardIndex == cardIndex) {
                val smart = SpiderEngine.smartMove(state.game, sel)
                if (smart != null) {
                    applyMove(smart, isCardPlace = true)
                } else {
                    _uiState.update { it.copy(selection = null) }
                }
                return
            }

            val moved = SpiderEngine.moveTableauToTableau(
                state = state.game,
                sourceCol = sel.col,
                cardIndex = sel.cardIndex,
                targetCol = col
            )
            if (moved != null) {
                applyMove(moved, isCardPlace = true)
                return
            }
        }

        if (pile.isNotEmpty() && SpiderEngine.isValidMovableRun(pile, cardIndex)) {
            if (state.isHapticsEnabled) hapticManager.performRevealHaptic()
            _uiState.update {
                it.copy(
                    selection = SpiderSelection(col, cardIndex),
                    stockBlockedMessage = null
                )
            }
        } else {
            _uiState.update { it.copy(selection = null) }
        }
    }

    fun undo() {
        if (_uiState.value.status == GameStatus.WON) return
        val previous = undoStack.removeLastOrNull() ?: return
        if (_uiState.value.isSoundEnabled) soundManager.playClick()
        _uiState.update {
            it.copy(
                game = previous,
                selection = null,
                canUndo = undoStack.isNotEmpty(),
                stockBlockedMessage = null
            )
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

    fun setHandedness(handedness: Handedness) {
        AppSettingsStore.setSpiderHandedness(getApplication(), handedness)
        _uiState.update { it.copy(handedness = handedness) }
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

    private fun showStockBlockedNotice(message: String) {
        noticeJob?.cancel()
        _uiState.update { it.copy(stockBlockedMessage = message, selection = null) }
        noticeJob = viewModelScope.launch {
            delay(2600)
            _uiState.update { it.copy(stockBlockedMessage = null) }
        }
    }

    private fun applyMove(next: SpiderGameState, isCardPlace: Boolean) {
        val currentState = _uiState.value
        undoStack.add(currentState.game)
        if (currentState.status == GameStatus.READY) {
            startTimer()
        }

        if (currentState.isSoundEnabled) {
            if (isCardPlace) soundManager.playFlag() else soundManager.playClick()
        }
        if (currentState.isHapticsEnabled && isCardPlace) {
            hapticManager.performRevealHaptic()
        }

        if (next.isWon) {
            timerJob?.cancel()
            noticeJob?.cancel()
            currentGameRecorded = true
            val time = _uiState.value.timerSeconds
            val updatedStats = statsRepository.recordGameFinished(
                won = true,
                timeSeconds = time,
                moves = next.moves
            )
            val isNewBest = updatedStats.bestTimeSeconds == time
            val isFirstWinToday = AppSettingsStore.recordDailyWin(
                getApplication(),
                GameDestination.SPIDER_SOLITAIRE
            )
            val streak = AppSettingsStore.getDailyStreak(GameDestination.SPIDER_SOLITAIRE)

            if (currentState.isSoundEnabled) {
                soundManager.playVictory(viewModelScope)
            }

            _uiState.update {
                it.copy(
                    game = next,
                    status = GameStatus.WON,
                    selection = null,
                    canUndo = false,
                    stockBlockedMessage = null,
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
                    stockBlockedMessage = null
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
        noticeJob?.cancel()
        soundManager.release()
    }
}
