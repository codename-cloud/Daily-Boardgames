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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cards.EmptyCardSlot
import com.example.cards.PlayingCard
import com.example.cards.PlayingCardView
import com.example.cards.Suit
import com.example.common.ArcadeGameHUD
import com.example.common.ArcadePillButton
import com.example.common.ArcadeVictoryDialog
import com.example.common.rememberArcadeThemeColors
import com.example.minesweeper.ui.components.DailyStreakDialog
import com.example.minesweeper.ui.components.HapticSettingsDialog

@Composable
fun SolitaireScreen(
    viewModel: SolitaireViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    LaunchedEffect(Unit) {
        viewModel.refreshSharedSettings()
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
                        label = "Hint",
                        selected = false,
                        isOledMode = uiState.isOledMode,
                        onClick = { viewModel.requestHint() },
                        testTag = "solitaire_hint_button"
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
                            label = "Hint",
                            selected = false,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.requestHint() },
                            testTag = "solitaire_hint_button"
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

            // Board Playfield
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        horizontal = if (isLandscape) 8.dp else 6.dp,
                        vertical = if (isLandscape) 3.dp else 6.dp
                    )
                    .testTag("solitaire_board")
            ) {
                val gap = 4.dp
                val boardMaxHeight = maxHeight
                val rowSpacing = if (isLandscape) 6.dp else 10.dp
                val widthBasedCardWidth = ((maxWidth - gap * 6) / 7f).coerceAtMost(74.dp)
                val maxCardHeightForLayout = if (isLandscape) {
                    (boardMaxHeight * 0.28f).coerceAtLeast(48.dp)
                } else {
                    200.dp
                }
                val cardWidth = minOf(widthBasedCardWidth, maxCardHeightForLayout * 0.70f)
                val cardHeight = cardWidth / 0.70f
                val cornerFont = (cardWidth.value * 0.24f).coerceIn(10f, 15f).sp
                val centerFont = (cardWidth.value * 0.42f).coerceIn(16f, 26f).sp

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(rowSpacing)
                ) {
                    // Top Row: Stock + Waste + Spacer + 4 Foundations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Stock
                        Box(modifier = Modifier.width(cardWidth)) {
                            if (game.stock.isNotEmpty()) {
                                PlayingCardView(
                                    card = game.stock.last(),
                                    isOledMode = uiState.isOledMode,
                                    isHinted = uiState.hint?.source == SolitaireSelection.Waste && game.waste.isEmpty(),
                                    cornerFontSize = cornerFont,
                                    centerFontSize = centerFont,
                                    onClick = { viewModel.onStockClick() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("solitaire_stock_pile")
                                )
                            } else {
                                EmptyCardSlot(
                                    label = "↻",
                                    isOledMode = uiState.isOledMode,
                                    onClick = { viewModel.onStockClick() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("solitaire_stock_pile")
                                )
                            }
                        }

                        // Waste (fan up to 3 visible cards in Draw 3 mode)
                        Box(
                            modifier = Modifier
                                .width(cardWidth * 1.45f)
                                .height(cardHeight)
                                .testTag("solitaire_waste_pile")
                        ) {
                            if (game.waste.isEmpty()) {
                                EmptyCardSlot(
                                    label = "",
                                    isOledMode = uiState.isOledMode,
                                    modifier = Modifier.width(cardWidth)
                                )
                            } else {
                                val visibleWaste = if (game.drawMode == SolitaireDrawMode.DRAW_3) {
                                    game.waste.takeLast(3)
                                } else {
                                    listOf(game.waste.last())
                                }
                                visibleWaste.forEachIndexed { idx, card ->
                                    val isTop = idx == visibleWaste.lastIndex
                                    PlayingCardView(
                                        card = card,
                                        isOledMode = uiState.isOledMode,
                                        isSelected = isTop && uiState.selection == SolitaireSelection.Waste,
                                        isHinted = isTop && uiState.hint?.source == SolitaireSelection.Waste,
                                        cornerFontSize = cornerFont,
                                        centerFontSize = centerFont,
                                        onClick = if (isTop) ({ viewModel.onWasteClick() }) else null,
                                        modifier = Modifier
                                            .width(cardWidth)
                                            .offset(x = (idx * 12).dp)
                                    )
                                }
                            }
                        }

                        // 4 Foundations
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalAlignment = Alignment.Top
                        ) {
                            val suitHints = listOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)
                            for (f in 0..3) {
                                val pile = game.foundations[f]
                                val topCard = pile.lastOrNull()
                                Box(modifier = Modifier.width(cardWidth)) {
                                    if (topCard != null) {
                                        PlayingCardView(
                                            card = topCard,
                                            isOledMode = uiState.isOledMode,
                                            isSelected = uiState.selection == SolitaireSelection.Foundation(f),
                                            cornerFontSize = cornerFont,
                                            centerFontSize = centerFont,
                                            onClick = { viewModel.onFoundationClick(f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("solitaire_foundation_$f")
                                        )
                                    } else {
                                        EmptyCardSlot(
                                            label = "A",
                                            isOledMode = uiState.isOledMode,
                                            tint = if (suitHints[f].isRed) Color(0xFFEF4444).copy(alpha = 0.55f) else null,
                                            onClick = { viewModel.onFoundationClick(f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("solitaire_foundation_$f")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7 Tableau Columns
                    val tableauAvailableHeight = (boardMaxHeight - cardHeight - rowSpacing).coerceAtLeast(48.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        for (col in 0..6) {
                            val pile = game.tableau[col]
                            Box(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(tableauAvailableHeight)
                                    .testTag("solitaire_tableau_$col")
                            ) {
                                if (pile.isEmpty()) {
                                    EmptyCardSlot(
                                        label = "K",
                                        isOledMode = uiState.isOledMode,
                                        onClick = { viewModel.onTableauCardClick(col, 0) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    val offsets = computeCascadeOffsets(
                                        pile = pile,
                                        availableHeight = tableauAvailableHeight,
                                        cardHeight = cardHeight
                                    )
                                    pile.forEachIndexed { idx, card ->
                                        val sel = uiState.selection
                                        val isSelected = sel is SolitaireSelection.Tableau &&
                                            sel.col == col && idx >= sel.cardIndex
                                        val hintSrc = uiState.hint?.source
                                        val isHinted = hintSrc is SolitaireSelection.Tableau &&
                                            hintSrc.col == col && idx >= hintSrc.cardIndex

                                        PlayingCardView(
                                            card = card,
                                            isOledMode = uiState.isOledMode,
                                            isSelected = isSelected,
                                            isHinted = isHinted,
                                            cornerFontSize = cornerFont,
                                            centerFontSize = centerFont,
                                            onClick = { viewModel.onTableauCardClick(col, idx) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .offset(y = offsets[idx])
                                        )
                                    }
                                }
                            }
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
