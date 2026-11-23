package com.example.minesweeper.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minesweeper.model.GameStatus
import com.example.minesweeper.ui.components.DailyStreakDialog
import com.example.minesweeper.ui.components.GameHUD
import com.example.minesweeper.ui.components.GameOverDialog
import com.example.minesweeper.ui.components.HapticSettingsDialog
import com.example.minesweeper.ui.components.MinesweeperBoard
import com.example.minesweeper.ui.components.StatsDialog
import com.example.minesweeper.viewmodel.MinesweeperViewModel

@Composable
fun MinesweeperScreen(
    viewModel: MinesweeperViewModel,
    onBackToHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBackToHome != null) {
        BackHandler(onBack = onBackToHome)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSettingsAndStreak()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // When the screen is physically rotated, synchronize the board orientation without resetting game state
    LaunchedEffect(isLandscape) {
        viewModel.setBoardOrientation(toLandscape = isLandscape)
    }

    var showGameOverDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.status) {
        if (uiState.status == GameStatus.WON || uiState.status == GameStatus.LOST) {
            showGameOverDialog = true
        } else {
            showGameOverDialog = false
        }
    }

    // Outer screen background: Pure #000000 in OLED mode, slate in dark mode, light slate in light mode
    val screenBackgroundColor = when {
        uiState.isOledMode -> Color.Black
        isDark -> Color(0xFF0F172A)
        else -> Color(0xFFE2E8F0)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBackgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Adaptive Game HUD (compact single-row in landscape, clean 3-row in portrait)
            GameHUD(
                status = uiState.status,
                remainingMines = uiState.remainingMines,
                timerSeconds = uiState.timerSeconds,
                currentDifficulty = uiState.difficulty,
                interactionMode = uiState.interactionMode,
                isSoundEnabled = uiState.isSoundEnabled,
                isOledMode = uiState.isOledMode,
                dailyStreak = uiState.dailyStreak,
                onResetClick = { viewModel.startNewGame() },
                onSelectDifficulty = { viewModel.setDifficulty(it) },
                onToggleExpertOrientation = { viewModel.toggleOrientation() },
                onToggleInteractionMode = { viewModel.toggleInteractionMode() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleOledMode = { viewModel.toggleOledMode() },
                onOpenDailyStreak = { viewModel.showDailyStreakDialog(true) },
                onOpenHapticSettings = { viewModel.showHapticSettingsDialog(true) },
                onOpenStats = { viewModel.showStatsDialog(true) },
                onBackToHome = onBackToHome
            )

            // Board occupies all remaining vertical & horizontal space; board colors are strictly preserved
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = if (isLandscape) 14.dp else 10.dp)
                    .padding(top = 2.dp, bottom = if (isLandscape) 6.dp else 12.dp),
                contentAlignment = Alignment.Center
            ) {
                MinesweeperBoard(
                    board = uiState.board,
                    fillScreen = uiState.fillScreen,
                    isOledMode = uiState.isOledMode,
                    onCellClick = { r, c -> viewModel.onCellPrimaryAction(r, c) },
                    onCellLongClick = { r, c -> viewModel.onCellSecondaryAction(r, c) }
                )
            }
        }
    }

    // Daily Streak Dialog: Pops up after winning the first board of the day (or when tapped in HUD)
    if (uiState.showDailyStreakDialog) {
        DailyStreakDialog(
            streak = uiState.dailyStreak,
            isOledMode = uiState.isOledMode,
            gameLabel = "Minesweeper",
            onDismiss = {
                viewModel.showDailyStreakDialog(false)
            }
        )
    } else if (showGameOverDialog) {
        GameOverDialog(
            status = uiState.status,
            timerSeconds = uiState.timerSeconds,
            difficulty = uiState.difficulty,
            isNewBestTime = uiState.isNewBestTime,
            onPlayAgain = {
                showGameOverDialog = false
                viewModel.startNewGame()
            },
            onDismiss = {
                showGameOverDialog = false
            }
        )
    }

    // Settings & Statistics Dialogs
    if (uiState.showHapticSettingsDialog) {
        HapticSettingsDialog(
            isHapticsEnabled = uiState.isHapticsEnabled,
            isFlagHapticsEnabled = uiState.isFlagHapticsEnabled,
            hapticStrength = uiState.hapticStrength,
            isOledMode = uiState.isOledMode,
            onToggleMasterHaptics = { viewModel.setHapticsEnabled(it) },
            onToggleFlagHaptics = { viewModel.setFlagHapticsEnabled(it) },
            onSelectHapticStrength = { viewModel.setHapticStrength(it) },
            onTestFlagHaptic = { viewModel.testFlagHaptic() },
            onToggleOledMode = { viewModel.setOledMode(it) },
            onDismiss = { viewModel.showHapticSettingsDialog(false) }
        )
    }

    if (uiState.showStatsDialog) {
        StatsDialog(
            stats = uiState.stats,
            dailyStreak = uiState.dailyStreak,
            onResetStats = { viewModel.resetStats(it) },
            onDismiss = { viewModel.showStatsDialog(false) }
        )
    }
}
