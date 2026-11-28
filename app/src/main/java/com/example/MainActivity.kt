package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.AppSettingsStore
import com.example.common.GameStatus
import com.example.freecell.FreeCellScreen
import com.example.freecell.FreeCellViewModel
import com.example.home.GameDestination
import com.example.home.GameMenuEntry
import com.example.home.StartScreen
import com.example.minesweeper.ui.MinesweeperScreen
import com.example.minesweeper.viewmodel.MinesweeperViewModel
import com.example.solitaire.SolitaireScreen
import com.example.solitaire.SolitaireViewModel
import com.example.spider.SpiderScreen
import com.example.spider.SpiderViewModel
import com.example.sudoku.SudokuScreen
import com.example.sudoku.SudokuViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val minesweeperViewModel: MinesweeperViewModel by viewModels()
    private val solitaireViewModel: SolitaireViewModel by viewModels()
    private val freeCellViewModel: FreeCellViewModel by viewModels()
    private val spiderViewModel: SpiderViewModel by viewModels()
    private val sudokuViewModel: SudokuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettingsStore.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val appSettings by AppSettingsStore.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveIsDark = appSettings.resolveIsDark(systemDark)

            LaunchedEffect(effectiveIsDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (effectiveIsDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (effectiveIsDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            MyApplicationTheme(darkTheme = effectiveIsDark) {
                var activeGame by rememberSaveable { mutableStateOf<GameDestination?>(null) }

                Crossfade(
                    targetState = activeGame,
                    animationSpec = tween(durationMillis = 200),
                    label = "game_navigation"
                ) { destination ->
                    when (destination) {
                        null -> HomeDestinationContent(
                            minesweeperViewModel = minesweeperViewModel,
                            solitaireViewModel = solitaireViewModel,
                            freeCellViewModel = freeCellViewModel,
                            spiderViewModel = spiderViewModel,
                            sudokuViewModel = sudokuViewModel,
                            onSelectGame = { selected -> activeGame = selected }
                        )

                        GameDestination.MINESWEEPER -> MinesweeperScreen(
                            viewModel = minesweeperViewModel,
                            onBackToHome = { activeGame = null }
                        )

                        GameDestination.SOLITAIRE -> SolitaireScreen(
                            viewModel = solitaireViewModel,
                            onBackToHome = { activeGame = null }
                        )

                        GameDestination.FREECELL -> FreeCellScreen(
                            viewModel = freeCellViewModel,
                            onBackToHome = { activeGame = null }
                        )

                        GameDestination.SPIDER_SOLITAIRE -> SpiderScreen(
                            viewModel = spiderViewModel,
                            onBackToHome = { activeGame = null }
                        )

                        GameDestination.SUDOKU -> SudokuScreen(
                            viewModel = sudokuViewModel,
                            onBackToHome = { activeGame = null }
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HomeDestinationContent(
    minesweeperViewModel: MinesweeperViewModel,
    solitaireViewModel: SolitaireViewModel,
    freeCellViewModel: FreeCellViewModel,
    spiderViewModel: SpiderViewModel,
    sudokuViewModel: SudokuViewModel,
    onSelectGame: (GameDestination) -> Unit
) {
    val mineState by minesweeperViewModel.uiState.collectAsStateWithLifecycle()
    val solitaireState by solitaireViewModel.uiState.collectAsStateWithLifecycle()
    val freeCellState by freeCellViewModel.uiState.collectAsStateWithLifecycle()
    val spiderState by spiderViewModel.uiState.collectAsStateWithLifecycle()
    val sudokuState by sudokuViewModel.uiState.collectAsStateWithLifecycle()

    val mineWins = mineState.stats.values.sumOf { it.won }
    val sudokuWins = sudokuState.stats.values.sumOf { it.won }

    val menuEntries = listOf(
        GameMenuEntry(
            destination = GameDestination.MINESWEEPER,
            title = "Minesweeper",
            subtitle = "Guess-Free Grid Logic",
            statsSummary = "Wins: $mineWins • ${mineState.difficulty.name}",
            dailyStreak = mineState.dailyStreak,
            isInProgress = mineState.status == GameStatus.RUNNING,
            accentColor = Color(0xFF2563EB),
            imageRes = R.drawable.simple_minesweeper_thumb_1795482654311,
            testTag = "game_card_minesweeper"
        ),
        GameMenuEntry(
            destination = GameDestination.SOLITAIRE,
            title = "Solitaire",
            subtitle = "Klondike • Draw 1 & 3",
            statsSummary = "Wins: ${solitaireState.stats.won}" +
                (solitaireState.stats.bestTimeSeconds?.let { " • Best ${it}s" } ?: ""),
            dailyStreak = solitaireState.dailyStreak,
            isInProgress = solitaireState.status == GameStatus.RUNNING,
            accentColor = Color(0xFF16A34A),
            imageRes = R.drawable.simple_solitaire_thumb_1795482654755,
            testTag = "game_card_solitaire"
        ),
        GameMenuEntry(
            destination = GameDestination.FREECELL,
            title = "FreeCell",
            subtitle = "4 Cells • Open Strategy",
            statsSummary = "Wins: ${freeCellState.stats.won}" +
                (freeCellState.stats.bestTimeSeconds?.let { " • Best ${it}s" } ?: ""),
            dailyStreak = freeCellState.dailyStreak,
            isInProgress = freeCellState.status == GameStatus.RUNNING,
            accentColor = Color(0xFF0D9488),
            imageRes = R.drawable.simple_freecell_thumb_1795482655874,
            testTag = "game_card_freecell"
        ),
        GameMenuEntry(
            destination = GameDestination.SPIDER_SOLITAIRE,
            title = "Spider Solitaire",
            subtitle = "10 Columns • 1, 2 & 4 Suits",
            statsSummary = "Wins: ${spiderState.stats.won} • ${spiderState.game.suitMode.label}",
            dailyStreak = spiderState.dailyStreak,
            isInProgress = spiderState.status == GameStatus.RUNNING,
            accentColor = Color(0xFF7C3AED),
            imageRes = R.drawable.img_game_spider_solitaire,
            testTag = "game_card_spider_solitaire"
        ),
        GameMenuEntry(
            destination = GameDestination.SUDOKU,
            title = "Sudoku",
            subtitle = "9×9 Unique Number Grid",
            statsSummary = "Solved: $sudokuWins • ${sudokuState.difficulty.label}",
            dailyStreak = sudokuState.dailyStreak,
            isInProgress = sudokuState.status == GameStatus.RUNNING,
            accentColor = Color(0xFFD97706),
            imageRes = R.drawable.simple_sudoku_thumb_1795482655682,
            testTag = "game_card_sudoku"
        )
    )

    StartScreen(
        games = menuEntries,
        onSelectGame = onSelectGame
    )
}
