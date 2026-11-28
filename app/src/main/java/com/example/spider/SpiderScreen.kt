package com.example.spider

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cards.AnimatedBoardCard
import com.example.cards.BoardCardPlacement
import com.example.cards.EmptyCardSlot
import com.example.cards.PlayingCard
import com.example.cards.PlayingCardView
import com.example.cards.Rank
import com.example.common.AppSettingsStore
import com.example.common.ArcadeGameHUD
import com.example.common.ArcadePillButton
import com.example.common.ArcadeVictoryDialog
import com.example.common.DailyStreakDialog
import com.example.common.GameTutorialDialog
import com.example.common.Handedness
import com.example.common.SettingsDialog
import com.example.common.rememberArcadeThemeColors
import com.example.home.GameDestination
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@Composable
fun SpiderScreen(
    viewModel: SpiderViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val context = LocalContext.current
    var showTutorialDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppSettingsStore.init(context)
        viewModel.refreshSharedSettings()
        if (!AppSettingsStore.hasSeenTutorial(GameDestination.SPIDER_SOLITAIRE)) {
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
                title = "Spider",
                leftCounterValue = game.moves,
                leftCounterTag = "spider_moves_counter",
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
                        testTag = "spider_undo_button"
                    )
                    ArcadePillButton(
                        label = "Replay",
                        selected = false,
                        isOledMode = uiState.isOledMode,
                        onClick = { viewModel.restartCurrentDeal() },
                        testTag = "spider_replay_button"
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
                        text = "${game.suitMode.label} • ${game.completedSuits.size}/8",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .testTag("spider_mode_label")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ArcadePillButton(
                            label = "Undo",
                            selected = false,
                            enabled = uiState.canUndo,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.undo() },
                            testTag = "spider_undo_button"
                        )
                        ArcadePillButton(
                            label = "Replay",
                            selected = false,
                            isOledMode = uiState.isOledMode,
                            onClick = { viewModel.restartCurrentDeal() },
                            testTag = "spider_replay_button"
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                SpiderPlayfield(
                    game = game,
                    dealGeneration = uiState.dealGeneration,
                    selection = uiState.selection,
                    handedness = uiState.handedness,
                    highlightEmptyColumns = uiState.stockBlockedMessage != null,
                    isOledMode = uiState.isOledMode,
                    isLandscape = isLandscape,
                    onStockClick = viewModel::onStockClick,
                    onTableauCardClick = viewModel::onTableauCardClick,
                    modifier = Modifier.fillMaxSize()
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.stockBlockedMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF991B1B).copy(alpha = 0.94f))
                            .border(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("spider_stock_blocked_banner")
                    ) {
                        Text(
                            text = uiState.stockBlockedMessage.orEmpty(),
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDailyStreakDialog) {
        DailyStreakDialog(
            streak = uiState.dailyStreak,
            isOledMode = uiState.isOledMode,
            gameLabel = "Spider Solitaire",
            onDismiss = { viewModel.showDailyStreakDialog(false) }
        )
    } else if (uiState.showVictoryDialog) {
        ArcadeVictoryDialog(
            title = "Spider Cleared!",
            subtitle = "Completed all 8 runs in ${uiState.timerSeconds}s (${game.moves} moves)!",
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Suit Difficulty",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SpiderSuitMode.entries.forEach { mode ->
                                ArcadePillButton(
                                    label = mode.label,
                                    selected = game.suitMode == mode,
                                    isOledMode = uiState.isOledMode,
                                    onClick = { viewModel.setSuitMode(mode) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "spider_suit_${mode.suitCount}_pill"
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Layout Handedness",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ArcadePillButton(
                                label = Handedness.LEFT_HANDED.label,
                                selected = uiState.handedness == Handedness.LEFT_HANDED,
                                isOledMode = uiState.isOledMode,
                                onClick = { viewModel.setHandedness(Handedness.LEFT_HANDED) },
                                modifier = Modifier.weight(1f),
                                testTag = "spider_left_handed_pill"
                            )
                            ArcadePillButton(
                                label = Handedness.RIGHT_HANDED.label,
                                selected = uiState.handedness == Handedness.RIGHT_HANDED,
                                isOledMode = uiState.isOledMode,
                                onClick = { viewModel.setHandedness(Handedness.RIGHT_HANDED) },
                                modifier = Modifier.weight(1f),
                                testTag = "spider_right_handed_pill"
                            )
                        }
                    }
                }
            }
        )
    }

    if (uiState.showStatsDialog) {
        SpiderStatsDialog(
            stats = uiState.stats,
            isOledMode = uiState.isOledMode,
            onReset = { viewModel.resetStats() },
            onDismiss = { viewModel.showStatsDialog(false) }
        )
    }

    if (showTutorialDialog) {
        GameTutorialDialog(
            game = GameDestination.SPIDER_SOLITAIRE,
            isOledMode = uiState.isOledMode,
            onDismiss = {
                AppSettingsStore.markTutorialSeen(context, GameDestination.SPIDER_SOLITAIRE)
                showTutorialDialog = false
            }
        )
    }
}

private data class SpiderCardEntry(
    val uid: Int,
    val placement: BoardCardPlacement,
    val tapCol: Int,
    val tapCardIndex: Int
)

private class PrevSpiderStateRef(
    var dealGen: Int = -1,
    var game: SpiderGameState? = null
)

@Composable
private fun SpiderPlayfield(
    game: SpiderGameState,
    dealGeneration: Int,
    selection: SpiderSelection?,
    handedness: Handedness,
    highlightEmptyColumns: Boolean,
    isOledMode: Boolean,
    isLandscape: Boolean,
    onStockClick: () -> Unit,
    onTableauCardClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val prevRef = remember { PrevSpiderStateRef() }
    val prevGame = if (prevRef.dealGen == dealGeneration) prevRef.game else null
    SideEffect {
        prevRef.dealGen = dealGeneration
        prevRef.game = game
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(
                horizontal = if (isLandscape) 8.dp else 5.dp,
                vertical = if (isLandscape) 3.dp else 6.dp
            )
            .testTag("spider_board")
    ) {
        val density = LocalDensity.current
        val gap = 2.5.dp
        val boardMaxWidth = maxWidth
        val boardMaxHeight = maxHeight
        val rowSpacing = if (isLandscape) 5.dp else 9.dp
        val widthBasedCardWidth = ((boardMaxWidth - gap * 9) / 10f).coerceAtMost(64.dp)
        val maxCardHeightForLayout = if (isLandscape) {
            (boardMaxHeight * 0.27f).coerceAtLeast(44.dp)
        } else {
            180.dp
        }
        val cardWidth = minOf(widthBasedCardWidth, maxCardHeightForLayout * 0.70f)
        val cardHeight = cardWidth / 0.70f
        val cornerFont = (cardWidth.value * 0.24f).coerceIn(9f, 14f).sp
        val centerFont = (cardWidth.value * 0.42f).coerceIn(14f, 24f).sp

        val boardSizeKey = remember(boardMaxWidth, boardMaxHeight) {
            with(density) {
                (boardMaxWidth.toPx().roundToInt().toLong() shl 32) or
                    (boardMaxHeight.toPx().roundToInt().toLong() and 0xFFFFFFFFL)
            }
        }

        val isLeftHanded = handedness == Handedness.LEFT_HANDED
        val colGap = ((boardMaxWidth - cardWidth * 10) / 9f).coerceAtLeast(0.dp)
        val colStep = cardWidth + colGap
        val stockOffsetPx = if (isLeftHanded) {
            Offset.Zero
        } else {
            with(density) { Offset((colStep * 9).toPx(), 0f) }
        }
        val tableauTopY = cardHeight + rowSpacing
        val tableauAvailableHeight = (boardMaxHeight - cardHeight - rowSpacing).coerceAtLeast(48.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Static top slots (8 completed suit piles + spacer + stock) and 10 tableau slots
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    val completedSlotsSection: @Composable () -> Unit = {
                        for (slot in 0 until 8) {
                            val completedSuit = game.completedSuits.getOrNull(slot)
                            if (completedSuit != null) {
                                PlayingCardView(
                                    card = PlayingCard(suit = completedSuit, rank = Rank.KING, isFaceUp = true),
                                    isOledMode = isOledMode,
                                    cornerFontSize = cornerFont,
                                    centerFontSize = centerFont,
                                    cornerRadius = 4.dp,
                                    cardWidth = cardWidth,
                                    cardHeight = cardHeight,
                                    modifier = Modifier.testTag("spider_completed_$slot")
                                )
                            } else {
                                EmptyCardSlot(
                                    label = "K",
                                    isOledMode = isOledMode,
                                    cornerRadius = 4.dp,
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .testTag("spider_completed_$slot")
                                )
                            }
                        }
                    }

                    val stockSlotSection: @Composable () -> Unit = {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .testTag("spider_stock_pile")
                        ) {
                            if (game.stock.isEmpty()) {
                                EmptyCardSlot(
                                    label = "✓",
                                    isOledMode = isOledMode,
                                    cornerRadius = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                PlayingCardView(
                                    card = game.stock.last().card,
                                    isOledMode = isOledMode,
                                    cornerRadius = 4.dp,
                                    cardWidth = cardWidth,
                                    cardHeight = cardHeight,
                                    onClick = onStockClick,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable(role = Role.Button, onClick = onStockClick)
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${game.stockDealsRemaining}",
                                        color = Color.White,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isLeftHanded) {
                        stockSlotSection()
                        Spacer(modifier = Modifier.width(cardWidth))
                        completedSlotsSection()
                    } else {
                        completedSlotsSection()
                        Spacer(modifier = Modifier.width(cardWidth))
                        stockSlotSection()
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    for (col in 0 until 10) {
                        val isEmptyCol = game.tableau[col].isEmpty()
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(tableauAvailableHeight)
                                .testTag("spider_tableau_$col")
                        ) {
                            EmptyCardSlot(
                                label = "",
                                isHighlighted = highlightEmptyColumns && isEmptyCol,
                                isOledMode = isOledMode,
                                cornerRadius = 4.dp,
                                onClick = remember(col, onTableauCardClick) { { onTableauCardClick(col, 0) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Layer 2: Keyed animated playing cards across the 10 tableau columns
            val dealtFromStock = prevGame != null && prevGame.stock.size > game.stock.size
            val prevStockUids = if (dealtFromStock) {
                prevGame!!.stock.map { it.uid }.toSet()
            } else {
                emptySet()
            }

            val placements = buildList {
                for (col in 0 until 10) {
                    val pile = game.tableau[col]
                    if (pile.isNotEmpty()) {
                        val offsets = computeSpiderCascadeOffsets(
                            pile = pile,
                            availableHeight = tableauAvailableHeight,
                            cardHeight = cardHeight
                        )
                        val colX = colStep * col
                        pile.forEachIndexed { idx, spiderCard ->
                            val isSelected = selection != null &&
                                selection.col == col &&
                                idx >= selection.cardIndex
                            val justDealt = dealtFromStock && spiderCard.uid in prevStockUids
                            val targetPx = with(density) {
                                Offset(colX.toPx(), (tableauTopY + offsets[idx]).toPx())
                            }
                            add(
                                SpiderCardEntry(
                                    uid = spiderCard.uid,
                                    placement = BoardCardPlacement(
                                        card = spiderCard.card,
                                        pileKey = "spider_tableau_$col",
                                        targetOffsetPx = targetPx,
                                        entranceOriginPx = if (justDealt) stockOffsetPx else null,
                                        entranceDelayMs = if (justDealt) col * 24 else 0,
                                        entranceFlip = justDealt,
                                        restZIndex = 10f + idx,
                                        isSelected = isSelected
                                    ),
                                    tapCol = col,
                                    tapCardIndex = idx
                                )
                            )
                        }
                    }
                }
            }

            for (entry in placements) {
                key("spider_${entry.uid}") {
                    val onCardClick = remember(entry.tapCol, entry.tapCardIndex, onTableauCardClick) {
                        { onTableauCardClick(entry.tapCol, entry.tapCardIndex) }
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

private fun computeSpiderCascadeOffsets(
    pile: List<SpiderCard>,
    availableHeight: Dp,
    cardHeight: Dp
): List<Dp> {
    if (pile.size <= 1) return listOf(0.dp)
    val defaultFaceDownStep = 7.dp
    val defaultFaceUpStep = 18.dp

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
private fun SpiderStatsDialog(
    stats: SpiderStats,
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
                text = "Spider Statistics",
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
                        SpiderStatLine("Games Played", stats.played.toString(), colors.secondaryText, colors.primaryText)
                        SpiderStatLine("Games Won", stats.won.toString(), colors.secondaryText, colors.primaryText)
                        SpiderStatLine("Win Rate", "${stats.winPercentage}%", colors.secondaryText, colors.primaryText)
                        SpiderStatLine("Best Time", stats.bestTimeSeconds?.let { "${it}s" } ?: "—", colors.secondaryText, colors.primaryText)
                        SpiderStatLine("Fewest Moves", stats.bestMoves?.toString() ?: "—", colors.secondaryText, colors.primaryText)
                        SpiderStatLine("Current Streak", stats.currentStreak.toString(), colors.secondaryText, colors.primaryText)
                        SpiderStatLine("Best Streak", stats.maxStreak.toString(), colors.secondaryText, colors.primaryText)
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
private fun SpiderStatLine(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = labelColor)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
