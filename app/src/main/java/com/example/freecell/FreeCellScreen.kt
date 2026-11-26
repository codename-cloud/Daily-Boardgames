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
import com.example.common.GameTutorialDialog
import com.example.common.rememberArcadeThemeColors
import com.example.home.GameDestination
import com.example.minesweeper.ui.components.DailyStreakDialog
import com.example.minesweeper.ui.components.HapticSettingsDialog
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
                title = "FreeCell #${game.dealNumber}",
                leftCounterValue = game.remainingCards,
                leftCounterTag = "freecell_remaining_counter",
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
                        label = "Hint",
                        selected = false,
                        isOledMode = uiState.isOledMode,
                        onClick = { viewModel.requestHint() },
                        testTag = "freecell_hint_button"
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
                            label = "Hint",
                            selected = false,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.requestHint() },
                            testTag = "freecell_hint_button"
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

            if (uiState.hint != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (uiState.isOledMode) Color(0xFF0A192F) else Color(0xFF1E3A8A).copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hint: ${uiState.hint?.description}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                                    isOledMode = uiState.isOledMode,
                                    onClick = { viewModel.onFreeCellClick(i) },
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .testTag("freecell_cell_$i")
                                )
                            }

                            for (f in 0..3) {
                                EmptyCardSlot(
                                    label = "A",
                                    isOledMode = uiState.isOledMode,
                                    onClick = { viewModel.onFoundationClick(f) },
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
                                        isOledMode = uiState.isOledMode,
                                        onClick = { viewModel.onTableauCardClick(col, 0) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Layer 2: Keyed animated playing cards
                    val placements = buildList {
                        // 4 Free Cells
                        for (i in 0..3) {
                            val card = game.freeCells[i]
                            if (card != null) {
                                val targetPx = with(density) { Offset((colStep * i).toPx(), 0f) }
                                add(
                                    BoardCardPlacement(
                                        card = card,
                                        pileKey = "freecell_$i",
                                        targetOffsetPx = targetPx,
                                        restZIndex = 10f,
                                        isSelected = uiState.selection == FreeCellSelection.FreeCell(i),
                                        isHinted = uiState.hint?.source == FreeCellSelection.FreeCell(i),
                                        onClick = { viewModel.onFreeCellClick(i) }
                                    )
                                )
                            }
                        }

                        // 4 Foundations (top 2 so underlying card stays visible when top card moves)
                        for (f in 0..3) {
                            val targetPx = with(density) { Offset((colStep * (4 + f)).toPx(), 0f) }
                            val visibleFoundation = game.foundations[f].takeLast(2)
                            visibleFoundation.forEachIndexed { idx, card ->
                                val isTop = idx == visibleFoundation.lastIndex
                                add(
                                    BoardCardPlacement(
                                        card = card,
                                        pileKey = "foundation_$f",
                                        targetOffsetPx = targetPx,
                                        restZIndex = 5f + idx,
                                        isSelected = isTop && uiState.selection == FreeCellSelection.Foundation(f),
                                        onClick = if (isTop) ({ viewModel.onFoundationClick(f) }) else null
                                    )
                                )
                            }
                        }

                        // 8 Tableau columns
                        for (col in 0..7) {
                            val pile = game.tableau[col]
                            if (pile.isNotEmpty()) {
                                val step = computeFreeCellStep(
                                    count = pile.size,
                                    availableHeight = tableauAvailableHeight,
                                    cardHeight = cardHeight
                                )
                                val colX = colStep * col
                                val sel = uiState.selection
                                val hintSrc = uiState.hint?.source
                                pile.forEachIndexed { idx, card ->
                                    val isSelected = sel is FreeCellSelection.Tableau &&
                                        sel.col == col && idx >= sel.cardIndex
                                    val isHinted = hintSrc is FreeCellSelection.Tableau &&
                                        hintSrc.col == col && idx >= hintSrc.cardIndex
                                    val targetPx = with(density) {
                                        Offset(colX.toPx(), (tableauTopY + step * idx).toPx())
                                    }
                                    add(
                                        BoardCardPlacement(
                                            card = card,
                                            pileKey = "tableau_$col",
                                            targetOffsetPx = targetPx,
                                            restZIndex = 10f + idx,
                                            isSelected = isSelected,
                                            isHinted = isHinted,
                                            onClick = { viewModel.onTableauCardClick(col, idx) }
                                        )
                                    )
                                }
                            }
                        }
                    }

                    for (placement in placements) {
                        key(placement.card.id) {
                            AnimatedBoardCard(
                                placement = placement,
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                dealGeneration = uiState.dealGeneration,
                                boardSizeKey = boardSizeKey,
                                isOledMode = uiState.isOledMode,
                                cornerFont = cornerFont,
                                centerFont = centerFont
                            )
                        }
                    }
                }
            }
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
        HapticSettingsDialog(
            isHapticsEnabled = uiState.isHapticsEnabled,
            isOledMode = uiState.isOledMode,
            isSoundEnabled = uiState.isSoundEnabled,
            onToggleSound = { viewModel.setSoundEnabled(it) },
            onToggleMasterHaptics = { viewModel.setHapticsEnabled(it) },
            onToggleOledMode = { viewModel.setOledMode(it) },
            showFlagHapticsSection = false,
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
