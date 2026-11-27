package com.example.solitaire

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cards.AnimatedBoardCard
import com.example.cards.BoardCardPlacement
import com.example.cards.EmptyCardSlot
import com.example.cards.PlayingCard
import com.example.cards.Suit
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
fun SolitaireScreen(
    viewModel: SolitaireViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val context = LocalContext.current
    var showTutorialDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppSettingsStore.init(context)
        viewModel.refreshSharedSettings()
        if (!AppSettingsStore.hasSeenTutorial(GameDestination.SOLITAIRE)) {
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
                title = "Solitaire",
                leftCounterValue = game.moves,
                leftCounterTag = "solitaire_moves_counter",
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
                    if (uiState.canAutoComplete && !uiState.isAutoCompleting) {
                        ArcadePillButton(
                            label = "Auto",
                            selected = true,
                            isOledMode = uiState.isOledMode,
                            accentColor = Color(0xFF16A34A),
                            onClick = { viewModel.triggerAutoComplete() },
                            testTag = "solitaire_autocomplete_button"
                        )
                    }
                    ArcadePillButton(
                        label = "Undo",
                        selected = false,
                        enabled = uiState.canUndo,
                        isOledMode = uiState.isOledMode,
                        onClick = { viewModel.undo() },
                        testTag = "solitaire_undo_button"
                    )
                    ArcadePillButton(
                        label = "Replay",
                        selected = false,
                        isOledMode = uiState.isOledMode,
                        onClick = { viewModel.restartCurrentDeal() },
                        testTag = "solitaire_replay_button"
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
                        text = game.drawMode.label,
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .testTag("solitaire_draw_mode_label")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (uiState.canAutoComplete && !uiState.isAutoCompleting) {
                            ArcadePillButton(
                                label = "Auto",
                                selected = true,
                                isOledMode = uiState.isOledMode,
                                accentColor = Color(0xFF16A34A),
                                onClick = { viewModel.triggerAutoComplete() },
                                testTag = "solitaire_autocomplete_button"
                            )
                        }
                        ArcadePillButton(
                            label = "Undo",
                            selected = false,
                            enabled = uiState.canUndo,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.undo() },
                            testTag = "solitaire_undo_button"
                        )
                        ArcadePillButton(
                            label = "Replay",
                            selected = false,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.restartCurrentDeal() },
                            testTag = "solitaire_replay_button"
                        )
                    }
                }
            }

            SolitairePlayfield(
                game = game,
                dealGeneration = uiState.dealGeneration,
                selection = uiState.selection,
                isOledMode = uiState.isOledMode,
                isLandscape = isLandscape,
                onStockClick = viewModel::onStockClick,
                onWasteClick = viewModel::onWasteClick,
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
            gameLabel = "Solitaire",
            onDismiss = { viewModel.showDailyStreakDialog(false) }
        )
    } else if (uiState.showVictoryDialog) {
        ArcadeVictoryDialog(
            title = "Solitaire Cleared!",
            subtitle = "Completed in ${uiState.timerSeconds}s with ${game.moves} moves!",
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Draw Mode",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.primaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArcadePillButton(
                            label = "Draw 1",
                            selected = game.drawMode == SolitaireDrawMode.DRAW_1,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.setDrawMode(SolitaireDrawMode.DRAW_1) },
                            modifier = Modifier.weight(1f),
                            testTag = "solitaire_draw1_pill"
                        )
                        ArcadePillButton(
                            label = "Draw 3",
                            selected = game.drawMode == SolitaireDrawMode.DRAW_3,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.setDrawMode(SolitaireDrawMode.DRAW_3) },
                            modifier = Modifier.weight(1f),
                            testTag = "solitaire_draw3_pill"
                        )
                    }
                }
            }
        )
    }

    if (uiState.showStatsDialog) {
        SolitaireStatsDialog(
            stats = uiState.stats,
            isOledMode = uiState.isOledMode,
            onReset = { viewModel.resetStats() },
            onDismiss = { viewModel.showStatsDialog(false) }
        )
    }

    if (showTutorialDialog) {
        GameTutorialDialog(
            game = GameDestination.SOLITAIRE,
            isOledMode = uiState.isOledMode,
            onDismiss = {
                AppSettingsStore.markTutorialSeen(context, GameDestination.SOLITAIRE)
                showTutorialDialog = false
            }
        )
    }
}

private data class SolitaireCardEntry(
    val placement: BoardCardPlacement,
    val tapType: Int,
    val arg1: Int = 0,
    val arg2: Int = 0
)

private const val SOL_TAP_NONE = 0
private const val SOL_TAP_STOCK = 1
private const val SOL_TAP_WASTE = 2
private const val SOL_TAP_FOUNDATION = 3
private const val SOL_TAP_TABLEAU = 4

@Composable
private fun SolitairePlayfield(
    game: SolitaireGameState,
    dealGeneration: Int,
    selection: SolitaireSelection?,
    isOledMode: Boolean,
    isLandscape: Boolean,
    onStockClick: () -> Unit,
    onWasteClick: () -> Unit,
    onFoundationClick: (Int) -> Unit,
    onTableauCardClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val prevDealAndGameState = remember { mutableStateOf<Pair<Int, SolitaireGameState>?>(null) }
    val prevGame = prevDealAndGameState.value
        ?.takeIf { it.first == dealGeneration }
        ?.second
    SideEffect {
        prevDealAndGameState.value = dealGeneration to game
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(
                horizontal = if (isLandscape) 8.dp else 6.dp,
                vertical = if (isLandscape) 3.dp else 6.dp
            )
            .testTag("solitaire_board")
    ) {
        val density = LocalDensity.current
        val gap = 4.dp
        val boardMaxWidth = maxWidth
        val boardMaxHeight = maxHeight
        val rowSpacing = if (isLandscape) 6.dp else 10.dp
        val widthBasedCardWidth = ((boardMaxWidth - gap * 6) / 7f).coerceAtMost(74.dp)
        val maxCardHeightForLayout = if (isLandscape) {
            (boardMaxHeight * 0.28f).coerceAtLeast(48.dp)
        } else {
            200.dp
        }
        val cardWidth = minOf(widthBasedCardWidth, maxCardHeightForLayout * 0.70f)
        val cardHeight = cardWidth / 0.70f
        val cornerFont = (cardWidth.value * 0.24f).coerceIn(10f, 15f).sp
        val centerFont = (cardWidth.value * 0.42f).coerceIn(16f, 26f).sp

        val boardSizeKey = remember(boardMaxWidth, boardMaxHeight) {
            with(density) {
                (boardMaxWidth.toPx().roundToInt().toLong() shl 32) or
                    (boardMaxHeight.toPx().roundToInt().toLong() and 0xFFFFFFFFL)
            }
        }

        val stockOffsetPx = Offset.Zero
        val wasteBaseOffsetPx = with(density) { Offset((cardWidth + 8.dp).toPx(), 0f) }
        val wasteStepPx = with(density) { 12.dp.toPx() }
        val foundationsStartX = (boardMaxWidth - (cardWidth * 4 + gap * 3)).coerceAtLeast(0.dp)
        val tableauTopY = cardHeight + rowSpacing
        val tableauAvailableHeight = (boardMaxHeight - cardHeight - rowSpacing).coerceAtLeast(48.dp)
        val tableauColGap = ((boardMaxWidth - cardWidth * 7) / 6f).coerceAtLeast(0.dp)
        val tableauColStep = cardWidth + tableauColGap

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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        EmptyCardSlot(
                            label = "↻",
                            isOledMode = isOledMode,
                            onClick = onStockClick,
                            modifier = Modifier
                                .width(cardWidth)
                                .testTag("solitaire_stock_pile")
                        )

                        Box(
                            modifier = Modifier
                                .width(cardWidth * 1.45f)
                                .height(cardHeight)
                                .testTag("solitaire_waste_pile")
                        ) {
                            EmptyCardSlot(
                                label = "",
                                isOledMode = isOledMode,
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalAlignment = Alignment.Top
                    ) {
                        val suitHints = listOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)
                        for (f in 0..3) {
                            EmptyCardSlot(
                                label = "A",
                                isOledMode = isOledMode,
                                tint = if (suitHints[f].isRed) Color(0xFFEF4444).copy(alpha = 0.55f) else null,
                                onClick = remember(f, onFoundationClick) { { onFoundationClick(f) } },
                                modifier = Modifier
                                    .width(cardWidth)
                                    .testTag("solitaire_foundation_$f")
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    for (col in 0..6) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(tableauAvailableHeight)
                                .testTag("solitaire_tableau_$col")
                        ) {
                            EmptyCardSlot(
                                label = "K",
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
                val stockVisible = game.stock.takeLast(2)
                val recycledFromWaste = prevGame != null && prevGame.stock.isEmpty() && game.stock.isNotEmpty()
                stockVisible.forEachIndexed { idx, card ->
                    val isTop = idx == stockVisible.lastIndex
                    add(
                        SolitaireCardEntry(
                            placement = BoardCardPlacement(
                                card = card,
                                pileKey = "stock",
                                targetOffsetPx = stockOffsetPx,
                                entranceOriginPx = if (isTop && recycledFromWaste) wasteBaseOffsetPx else null,
                                restZIndex = 1f + idx
                            ),
                            tapType = if (isTop) SOL_TAP_STOCK else SOL_TAP_NONE
                        )
                    )
                }

                val drawnFromStock = prevGame != null && prevGame.stock.size > game.stock.size
                val prevStockIds = if (drawnFromStock) prevGame!!.stock.map { it.id }.toSet() else emptySet()
                val visibleWaste = if (game.drawMode == SolitaireDrawMode.DRAW_3) {
                    game.waste.takeLast(3)
                } else {
                    game.waste.takeLast(2)
                }
                visibleWaste.forEachIndexed { idx, card ->
                    val isTop = idx == visibleWaste.lastIndex
                    val fanX = if (game.drawMode == SolitaireDrawMode.DRAW_3) idx * wasteStepPx else 0f
                    val justDrawn = drawnFromStock && card.id in prevStockIds
                    add(
                        SolitaireCardEntry(
                            placement = BoardCardPlacement(
                                card = card,
                                pileKey = "waste",
                                targetOffsetPx = Offset(wasteBaseOffsetPx.x + fanX, wasteBaseOffsetPx.y),
                                entranceOriginPx = if (justDrawn) stockOffsetPx else null,
                                entranceDelayMs = if (justDrawn && game.drawMode == SolitaireDrawMode.DRAW_3) idx * 35 else 0,
                                entranceFlip = justDrawn,
                                restZIndex = 20f + idx,
                                isSelected = isTop && selection == SolitaireSelection.Waste
                            ),
                            tapType = if (isTop) SOL_TAP_WASTE else SOL_TAP_NONE
                        )
                    )
                }

                for (f in 0..3) {
                    val foundationX = foundationsStartX + (cardWidth + gap) * f
                    val foundationOffsetPx = with(density) { Offset(foundationX.toPx(), 0f) }
                    val visibleFoundation = game.foundations[f].takeLast(2)
                    visibleFoundation.forEachIndexed { idx, card ->
                        val isTop = idx == visibleFoundation.lastIndex
                        add(
                            SolitaireCardEntry(
                                placement = BoardCardPlacement(
                                    card = card,
                                    pileKey = "foundation_$f",
                                    targetOffsetPx = foundationOffsetPx,
                                    restZIndex = 5f + idx,
                                    isSelected = isTop && selection == SolitaireSelection.Foundation(f)
                                ),
                                tapType = if (isTop) SOL_TAP_FOUNDATION else SOL_TAP_NONE,
                                arg1 = f
                            )
                        )
                    }
                }

                for (col in 0..6) {
                    val pile = game.tableau[col]
                    if (pile.isNotEmpty()) {
                        val offsets = computeCascadeOffsets(
                            pile = pile,
                            availableHeight = tableauAvailableHeight,
                            cardHeight = cardHeight
                        )
                        val colX = tableauColStep * col
                        pile.forEachIndexed { idx, card ->
                            val isSelected = selection is SolitaireSelection.Tableau &&
                                selection.col == col && idx >= selection.cardIndex
                            val targetPx = with(density) {
                                Offset(colX.toPx(), (tableauTopY + offsets[idx]).toPx())
                            }
                            add(
                                SolitaireCardEntry(
                                    placement = BoardCardPlacement(
                                        card = card,
                                        pileKey = "tableau_$col",
                                        targetOffsetPx = targetPx,
                                        restZIndex = 10f + idx,
                                        isSelected = isSelected
                                    ),
                                    tapType = SOL_TAP_TABLEAU,
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
                        onStockClick,
                        onWasteClick,
                        onFoundationClick,
                        onTableauCardClick
                    ) {
                        when (entry.tapType) {
                            SOL_TAP_STOCK -> onStockClick
                            SOL_TAP_WASTE -> onWasteClick
                            SOL_TAP_FOUNDATION -> ({ onFoundationClick(entry.arg1) })
                            SOL_TAP_TABLEAU -> ({ onTableauCardClick(entry.arg1, entry.arg2) })
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

private fun computeCascadeOffsets(
    pile: List<PlayingCard>,
    availableHeight: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp
): List<androidx.compose.ui.unit.Dp> {
    if (pile.size <= 1) return listOf(0.dp)
    val defaultFaceDownStep = 10.dp
    val defaultFaceUpStep = 24.dp

    var totalStep = 0.dp
    for (i in 0 until pile.lastIndex) {
        totalStep += if (pile[i].isFaceUp) defaultFaceUpStep else defaultFaceDownStep
    }
    val maxUsable = (availableHeight - cardHeight).coerceAtLeast(32.dp)
    val scale = if (totalStep > maxUsable && totalStep > 0.dp) {
        maxUsable.value / totalStep.value
    } else {
        1f
    }

    val result = mutableListOf(0.dp)
    var y = 0.dp
    for (i in 0 until pile.lastIndex) {
        val step = if (pile[i].isFaceUp) defaultFaceUpStep else defaultFaceDownStep
        y += step * scale
        result.add(y)
    }
    return result
}

@Composable
private fun SolitaireStatsDialog(
    stats: SolitaireStats,
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
                text = "Solitaire Statistics",
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
                        StatLine("Games Played", stats.played.toString(), colors.secondaryText, colors.primaryText)
                        StatLine("Games Won", stats.won.toString(), colors.secondaryText, colors.primaryText)
                        StatLine("Win Rate", "${stats.winPercentage}%", colors.secondaryText, colors.primaryText)
                        StatLine("Best Time", stats.bestTimeSeconds?.let { "${it}s" } ?: "—", colors.secondaryText, colors.primaryText)
                        StatLine("Fewest Moves", stats.bestMoves?.toString() ?: "—", colors.secondaryText, colors.primaryText)
                        StatLine("Current Streak", stats.currentStreak.toString(), colors.secondaryText, colors.primaryText)
                        StatLine("Best Streak", stats.maxStreak.toString(), colors.secondaryText, colors.primaryText)
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
private fun StatLine(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = labelColor)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
