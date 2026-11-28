package com.example.freecell

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cards.AnimatedBoardCard
import com.example.cards.BoardCardPlacement
import com.example.cards.EmptyCardSlot
import com.example.common.AppSettingsStore
import com.example.common.ArcadeGameHUD
import com.example.common.ArcadePillButton
import com.example.common.ArcadeVictoryDialog
import com.example.common.DailyStreakDialog
import com.example.common.GameTutorialDialog
import com.example.common.SettingsDialog
import com.example.common.rememberArcadeThemeColors
import com.example.home.GameDestination
import kotlin.math.roundToInt

@Composable
fun FreeCellScreen(
    viewModel: FreeCellViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val context = LocalContext.current
    var showTutorialDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppSettingsStore.init(context)
        viewModel.refreshSharedSettings()
        if (!AppSettingsStore.hasSeenTutorial(GameDestination.FREECELL)) {
            showTutorialDialog = true
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = rememberArcadeThemeColors(uiState.isOledMode)
    val game = uiState.game
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.screenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
        ) {
            ArcadeGameHUD(
                title = "FreeCell",
                leftCounterValue = game.moves,
                leftCounterTag = "freecell_moves_counter",
                timerSeconds = uiState.timerSeconds,
                status = uiState.status,
                isOledMode = uiState.isOledMode,
                dailyStreak = uiState.dailyStreak,
                onBackClick = onBackToHome,
                onResetClick = { viewModel.startNewGame() },
                onOpenSettings = { viewModel.showSettingsDialog(true) },
                onOpenDailyStreak = { viewModel.showDailyStreakDialog(true) },
                onOpenStats = { viewModel.showStatsDialog(true) },
                landscapeActionsContent = {
                    ArcadePillButton(
                        label = "Undo",
                        selected = false,
                        enabled = uiState.canUndo,
                        isOledMode = uiState.isOledMode,
                        onClick = { viewModel.undo() },
                        testTag = "freecell_undo_button"
                    )
                    ArcadePillButton(
                        label = "Replay",
                        selected = false,
                        isOledMode = uiState.isOledMode,
                        onClick = { viewModel.restartCurrentDeal() },
                        testTag = "freecell_replay_button"
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isAutoFoundationEnabled) "Auto-Move: ON" else "Auto-Move: OFF",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .testTag("freecell_auto_status")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ArcadePillButton(
                            label = "Undo",
                            selected = false,
                            enabled = uiState.canUndo,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.undo() },
                            testTag = "freecell_undo_button"
                        )
                        ArcadePillButton(
                            label = "Replay",
                            selected = false,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.restartCurrentDeal() },
                            testTag = "freecell_replay_button"
                        )
                    }
                }
            }

            FreeCellPlayfield(
                game = game,
                dealGeneration = uiState.dealGeneration,
                selection = uiState.selection,
                isOledMode = uiState.isOledMode,
                isLandscape = isLandscape,
                onFreeCellClick = viewModel::onFreeCellClick,
                onFoundationClick = viewModel::onFoundationClick,
                onTableauCardClick = viewModel::onTableauCardClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    if (uiState.showDailyStreakDialog) {
        DailyStreakDialog(
            streak = uiState.dailyStreak,
            isOledMode = uiState.isOledMode,
            gameLabel = "FreeCell",
            onDismiss = { viewModel.showDailyStreakDialog(false) }
        )
    } else if (uiState.showVictoryDialog) {
        ArcadeVictoryDialog(
            title = "FreeCell Cleared!",
            subtitle = "Solved Deal #${game.dealNumber} in ${uiState.timerSeconds}s (${game.moves} moves).",
            isNewBest = uiState.isNewBestTime,
            isOledMode = uiState.isOledMode,
            onPlayAgain = { viewModel.startNewGame() },
            onBackToHome = {
                viewModel.dismissVictoryDialog()
                onBackToHome()
            },
            onDismiss = { viewModel.dismissVictoryDialog() }
        )
    }

    if (uiState.showSettingsDialog) {
        SettingsDialog(
            isOledMode = uiState.isOledMode,
            onOpenTutorial = {
                viewModel.showSettingsDialog(false)
                showTutorialDialog = true
            },
            onDismiss = { viewModel.showSettingsDialog(false) },
            gameOptionsContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Auto-Move to Foundation",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Automatically move safe cards up to foundations",
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                    }
                    Switch(
                        checked = uiState.isAutoFoundationEnabled,
                        onCheckedChange = { viewModel.setAutoFoundationEnabled(it) },
                        modifier = Modifier.testTag("freecell_auto_switch")
                    )
                }
            }
        )
    }

    if (uiState.showStatsDialog) {
        FreeCellStatsDialog(
            stats = uiState.stats,
            isOledMode = uiState.isOledMode,
            onReset = { viewModel.resetStats() },
            onDismiss = { viewModel.showStatsDialog(false) }
        )
    }

    if (showTutorialDialog) {
        GameTutorialDialog(
            game = GameDestination.FREECELL,
            isOledMode = uiState.isOledMode,
            onDismiss = {
                AppSettingsStore.markTutorialSeen(context, GameDestination.FREECELL)
                showTutorialDialog = false
            }
        )
    }
}

private data class FreeCellCardEntry(
    val placement: BoardCardPlacement,
    val tapType: Int,
    val arg1: Int = 0,
    val arg2: Int = 0
)

private const val FC_TAP_NONE = 0
private const val FC_TAP_FREECELL = 1
private const val FC_TAP_FOUNDATION = 2
private const val FC_TAP_TABLEAU = 3

@Composable
private fun FreeCellPlayfield(
    game: FreeCellGameState,
    dealGeneration: Int,
    selection: FreeCellSelection?,
    isOledMode: Boolean,
    isLandscape: Boolean,
    onFreeCellClick: (Int) -> Unit,
    onFoundationClick: (Int) -> Unit,
    onTableauCardClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(
                horizontal = if (isLandscape) 8.dp else 5.dp,
                vertical = if (isLandscape) 3.dp else 6.dp
            )
            .testTag("freecell_board")
    ) {
        val density = LocalDensity.current
        val gap = if (isLandscape) 4.dp else 3.dp
        val boardMaxWidth = maxWidth
        val boardMaxHeight = maxHeight
        val rowSpacing = if (isLandscape) 6.dp else 10.dp
        val widthBasedCardWidth = ((boardMaxWidth - gap * 7) / 8f).coerceAtMost(70.dp)
        val maxCardHeightForLayout = if (isLandscape) {
            (boardMaxHeight * 0.28f).coerceAtLeast(48.dp)
        } else {
            200.dp
        }
        val cardWidth = minOf(widthBasedCardWidth, maxCardHeightForLayout * 0.70f)
        val cardHeight = cardWidth / 0.70f
        val cornerFont = (cardWidth.value * 0.25f).coerceIn(9.5f, 14f).sp
        val centerFont = (cardWidth.value * 0.42f).coerceIn(14f, 24f).sp

        val boardSizeKey = remember(boardMaxWidth, boardMaxHeight) {
            with(density) {
                (boardMaxWidth.toPx().roundToInt().toLong() shl 32) or
                    (boardMaxHeight.toPx().roundToInt().toLong() and 0xFFFFFFFFL)
            }
        }

        val colGap = ((boardMaxWidth - cardWidth * 8) / 7f).coerceAtLeast(0.dp)
        val colStep = cardWidth + colGap
        val tableauTopY = cardHeight + rowSpacing
        val tableauAvailableHeight = (boardMaxHeight - cardHeight - rowSpacing).coerceAtLeast(48.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Static empty slots & test-tagged pile targets
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    for (i in 0..3) {
                        EmptyCardSlot(
                            label = "FC",
                            isOledMode = isOledMode,
                            onClick = remember(i, onFreeCellClick) { { onFreeCellClick(i) } },
                            modifier = Modifier
                                .width(cardWidth)
                                .testTag("freecell_cell_$i")
                        )
                    }

                    for (f in 0..3) {
                        EmptyCardSlot(
                            label = "A",
                            isOledMode = isOledMode,
                            onClick = remember(f, onFoundationClick) { { onFoundationClick(f) } },
                            modifier = Modifier
                                .width(cardWidth)
                                .testTag("freecell_foundation_$f")
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    for (col in 0..7) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(tableauAvailableHeight)
                                .testTag("freecell_tableau_$col")
                        ) {
                            EmptyCardSlot(
                                label = "",
                                isOledMode = isOledMode,
                                onClick = remember(col, onTableauCardClick) { { onTableauCardClick(col, 0) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Layer 2: Keyed animated playing cards
            val placements = buildList {
                for (i in 0..3) {
                    val card = game.freeCells[i]
                    if (card != null) {
                        val targetPx = with(density) { Offset((colStep * i).toPx(), 0f) }
                        add(
                            FreeCellCardEntry(
                                placement = BoardCardPlacement(
                                    card = card,
                                    pileKey = "freecell_$i",
                                    targetOffsetPx = targetPx,
                                    restZIndex = 10f,
                                    isSelected = selection == FreeCellSelection.FreeCell(i)
                                ),
                                tapType = FC_TAP_FREECELL,
                                arg1 = i
                            )
                        )
                    }
                }

                for (f in 0..3) {
                    val targetPx = with(density) { Offset((colStep * (4 + f)).toPx(), 0f) }
                    val visibleFoundation = game.foundations[f].takeLast(4)
                    visibleFoundation.forEachIndexed { idx, card ->
                        val isTop = idx == visibleFoundation.lastIndex
                        add(
                            FreeCellCardEntry(
                                placement = BoardCardPlacement(
                                    card = card,
                                    pileKey = "foundation_$f",
                                    targetOffsetPx = targetPx,
                                    restZIndex = 5f + card.rank.value * 0.01f,
                                    isSelected = isTop && selection == FreeCellSelection.Foundation(f)
                                ),
                                tapType = if (isTop) FC_TAP_FOUNDATION else FC_TAP_NONE,
                                arg1 = f
                            )
                        )
                    }
                }

                for (col in 0..7) {
                    val pile = game.tableau[col]
                    if (pile.isNotEmpty()) {
                        val step = computeFreeCellStep(
                            count = pile.size,
                            availableHeight = tableauAvailableHeight,
                            cardHeight = cardHeight
                        )
                        val colX = colStep * col
                        pile.forEachIndexed { idx, card ->
                            val isSelected = selection is FreeCellSelection.Tableau &&
                                selection.col == col && idx >= selection.cardIndex
                            val targetPx = with(density) {
                                Offset(colX.toPx(), (tableauTopY + step * idx).toPx())
                            }
                            add(
                                FreeCellCardEntry(
                                    placement = BoardCardPlacement(
                                        card = card,
                                        pileKey = "tableau_$col",
                                        targetOffsetPx = targetPx,
                                        restZIndex = 10f + idx,
                                        isSelected = isSelected
                                    ),
                                    tapType = FC_TAP_TABLEAU,
                                    arg1 = col,
                                    arg2 = idx
                                )
                            )
                        }
                    }
                }
            }

            for (entry in placements) {
                key(entry.placement.card.id) {
                    val onCardClick = remember(
                        entry.tapType,
                        entry.arg1,
                        entry.arg2,
                        onFreeCellClick,
                        onFoundationClick,
                        onTableauCardClick
                    ) {
                        when (entry.tapType) {
                            FC_TAP_FREECELL -> ({ onFreeCellClick(entry.arg1) })
                            FC_TAP_FOUNDATION -> ({ onFoundationClick(entry.arg1) })
                            FC_TAP_TABLEAU -> ({ onTableauCardClick(entry.arg1, entry.arg2) })
                            else -> null
                        }
                    }
                    AnimatedBoardCard(
                        placement = entry.placement,
                        onClick = onCardClick,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        dealGeneration = dealGeneration,
                        boardSizeKey = boardSizeKey,
                        isOledMode = isOledMode,
                        cornerFont = cornerFont,
                        centerFont = centerFont
                    )
                }
            }
        }
    }
}

private fun computeFreeCellStep(
    count: Int,
    availableHeight: Dp,
    cardHeight: Dp
): Dp {
    if (count <= 1) return 0.dp
    val maxUsable = (availableHeight - cardHeight).coerceAtLeast(16.dp)
    val defaultStep = 23.dp
    val needed = defaultStep * (count - 1)
    return if (needed > maxUsable) maxUsable / (count - 1) else defaultStep
}

@Composable
private fun FreeCellStatsDialog(
    stats: FreeCellStats,
    isOledMode: Boolean,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberArcadeThemeColors(isOledMode)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogSurface,
        title = {
            Text(
                text = "FreeCell Statistics",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.primaryText
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOledMode) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FreeCellStatRow("Games Played", stats.played.toString(), colors.secondaryText, colors.primaryText)
                        FreeCellStatRow("Games Won", stats.won.toString(), colors.secondaryText, colors.primaryText)
                        FreeCellStatRow("Win Rate", "${stats.winPercentage}%", colors.secondaryText, colors.primaryText)
                        FreeCellStatRow("Best Time", stats.bestTimeSeconds?.let { "${it}s" } ?: "—", colors.secondaryText, colors.primaryText)
                        FreeCellStatRow("Fewest Moves", stats.bestMoves?.toString() ?: "—", colors.secondaryText, colors.primaryText)
                        FreeCellStatRow("Current Streak", stats.currentStreak.toString(), colors.secondaryText, colors.primaryText)
                        FreeCellStatRow("Best Streak", stats.maxStreak.toString(), colors.secondaryText, colors.primaryText)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReset) {
                        Text("Reset Stats", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FreeCellStatRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = labelColor)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
