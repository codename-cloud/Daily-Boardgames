package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.AppSettingsStore
import com.example.freecell.FreeCellScreen
import com.example.freecell.FreeCellViewModel
import com.example.home.GameDestination
import com.example.home.GameMenuEntry
import com.example.home.StartScreen
import com.example.minesweeper.model.GameStatus
import com.example.minesweeper.ui.MinesweeperScreen
import com.example.minesweeper.viewmodel.MinesweeperViewModel
import com.example.solitaire.SolitaireScreen
import com.example.solitaire.SolitaireViewModel
import com.example.sudoku.SudokuScreen
import com.example.sudoku.SudokuViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val minesweeperViewModel: MinesweeperViewModel by viewModels()
    private val solitaireViewModel: SolitaireViewModel by viewModels()
    private val freeCellViewModel: FreeCellViewModel by viewModels()
    private val sudokuViewModel: SudokuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettingsStore.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var activeGame by rememberSaveable { mutableStateOf<GameDestination?>(null) }

                val mineState by minesweeperViewModel.uiState.collectAsStateWithLifecycle()
                val solitaireState by solitaireViewModel.uiState.collectAsStateWithLifecycle()
                val freeCellState by freeCellViewModel.uiState.collectAsStateWithLifecycle()
                val sudokuState by sudokuViewModel.uiState.collectAsStateWithLifecycle()

                Crossfade(
                    targetState = activeGame,
                    animationSpec = tween(durationMillis = 200),
                    label = "game_navigation"
                ) { destination ->
                    when (destination) {
                        null -> {
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
                                onSelectGame = { selected -> activeGame = selected }
                            )
                        }

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
