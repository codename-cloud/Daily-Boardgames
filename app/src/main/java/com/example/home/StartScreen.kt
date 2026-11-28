package com.example.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.AppSettingsStore
import com.example.common.DailyStreak
import com.example.common.DailyStreakBadge
import com.example.common.DailyStreakDialog
import com.example.common.GitHubUpdateDialog
import com.example.common.GitHubUpdateManager
import com.example.common.HapticManager
import com.example.common.SettingsDialog
import com.example.common.SoundManager
import com.example.common.rememberArcadeThemeColors
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.OutfitFontFamily

enum class GameDestination {
    MINESWEEPER,
    SOLITAIRE,
    FREECELL,
    SPIDER_SOLITAIRE,
    SUDOKU
}

data class GameMenuEntry(
    val destination: GameDestination,
    val title: String,
    val subtitle: String,
    val statsSummary: String,
    val dailyStreak: DailyStreak,
    val isInProgress: Boolean,
    val accentColor: Color,
    @DrawableRes val imageRes: Int,
    val testTag: String
)

@Composable
fun StartScreen(
    games: List<GameMenuEntry>,
    onSelectGame: (GameDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by AppSettingsStore.settings.collectAsStateWithLifecycle()
    val dailyStreaks by AppSettingsStore.dailyStreaks.collectAsStateWithLifecycle()
    val updateState by GitHubUpdateManager.uiState.collectAsStateWithLifecycle()
    val colors = rememberArcadeThemeColors(settings.isOledMode)
    val isDark = LocalIsDarkTheme.current

    var selectedStreakEntry by remember { mutableStateOf<GameMenuEntry?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDailySweepCelebration by remember { mutableStateOf(false) }
    val hapticManager = remember { HapticManager(context) }
    val soundManager = remember { SoundManager() }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    LaunchedEffect(settings.githubRepo) {
        if (settings.githubRepo.isNotBlank()) {
            GitHubUpdateManager.autoCheckIfNeeded(settings.githubRepo)
        }
    }

    val trackedGames = games.filter { it.destination in settings.dailyWinsGames }
        .ifEmpty { games }
    val completedTodayCount = trackedGames.count { entry ->
        (dailyStreaks[entry.destination] ?: entry.dailyStreak).isWinAwardedToday
    }
    val totalTrackedGames = trackedGames.size

    LaunchedEffect(completedTodayCount, totalTrackedGames, settings.dailyWinsGames) {
        if (totalTrackedGames > 0 &&
            completedTodayCount >= totalTrackedGames &&
            AppSettingsStore.consumeDailySweepCelebrationIfReady(context)
        ) {
            showDailySweepCelebration = true
            if (settings.isSoundEnabled) {
                soundManager.playVictory(coroutineScope)
            }
            if (settings.isHapticsEnabled) {
                hapticManager.performFlagHaptic(settings.hapticStrength)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.screenBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Minesweeper-matched Top Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.hudBackground)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Boardgames",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Choose a classic puzzle or card game",
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableUpdate = updateState.availableUpdate
                        if (availableUpdate != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF16A34A))
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Open GitHub Update",
                                        onClick = { showUpdateDialog = true }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("home_update_available_badge"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Update available",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Update",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "App Settings",
                                tint = colors.primaryText
                            )
                        }
                    }
                }
            }

            // Adaptive Grid of Games
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                val columns = when {
                    maxWidth >= 900.dp -> 5
                    maxWidth >= 600.dp -> 3
                    else -> 2
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxSize()
                        .testTag("start_screen_game_grid"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Daily Wins summary strip across top
                    item(span = { GridItemSpan(columns) }) {
                        val allCompleted = totalTrackedGames > 0 && completedTodayCount >= totalTrackedGames
                        val counterBg = when {
                            allCompleted -> Color(0xFF16A34A).copy(alpha = if (isDark) 0.18f else 0.12f)
                            else -> colors.pillInactiveBg
                        }
                        val counterBorder = when {
                            allCompleted -> Color(0xFF16A34A).copy(alpha = if (isDark) 0.5f else 0.35f)
                            else -> colors.cardBorder
                        }
                        val counterTextColor = when {
                            allCompleted -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            completedTodayCount > 0 -> if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                            else -> colors.primaryText
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.cardBackground)
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Wins",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = colors.primaryText
                                )
                                if (totalTrackedGames < games.size) {
                                    Text(
                                        text = "$totalTrackedGames of ${games.size} games selected",
                                        fontSize = 11.sp,
                                        color = colors.secondaryText,
                                        modifier = Modifier.testTag("home_daily_wins_subset_label")
                                    )
                                }
                            }

                            val counterPulseScale by animateFloatAsState(
                                targetValue = if (showDailySweepCelebration) 1.14f else 1f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                                label = "daily_wins_counter_pulse"
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = counterPulseScale
                                        scaleY = counterPulseScale
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(counterBg)
                                    .border(1.dp, counterBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$completedTodayCount / $totalTrackedGames",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = counterTextColor,
                                    modifier = Modifier.testTag("home_today_counter")
                                )
                            }
                        }
                    }

                    items(games, key = { it.destination.name }) { entry ->
                        val liveStreak = dailyStreaks[entry.destination] ?: entry.dailyStreak
                        GameSelectionCard(
                            entry = entry.copy(dailyStreak = liveStreak),
                            isOledMode = settings.isOledMode,
                            isDark = isDark,
                            onClick = { onSelectGame(entry.destination) },
                            onStreakClick = { selectedStreakEntry = entry.copy(dailyStreak = liveStreak) }
                        )
                    }
                }
            }
        }
        }

        DailySweepCelebrationOverlay(
            visible = showDailySweepCelebration,
            completedCount = completedTodayCount,
            totalCount = totalTrackedGames,
            isOledMode = settings.isOledMode,
            isDark = isDark,
            onFinished = { showDailySweepCelebration = false }
        )
    }

    selectedStreakEntry?.let { entry ->
        DailyStreakDialog(
            streak = entry.dailyStreak,
            isOledMode = settings.isOledMode,
            gameLabel = entry.title,
            onDismiss = { selectedStreakEntry = null }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            title = "App Settings",
            isHapticsEnabled = settings.isHapticsEnabled,
            isFlagHapticsEnabled = settings.isFlagHapticsEnabled,
            hapticStrength = settings.hapticStrength,
            isOledMode = settings.isOledMode,
            isSoundEnabled = settings.isSoundEnabled,
            onToggleSound = { AppSettingsStore.setSoundEnabled(context, it) },
            onToggleMasterHaptics = { AppSettingsStore.setHapticsEnabled(context, it) },
            onToggleFlagHaptics = { AppSettingsStore.setFlagHapticsEnabled(context, it) },
            onSelectHapticStrength = { AppSettingsStore.setHapticStrength(context, it) },
            onTestFlagHaptic = { hapticManager.performFlagHaptic(settings.hapticStrength) },
            onToggleOledMode = { AppSettingsStore.setOledMode(context, it) },
            showThemeSection = true,
            showSoundAndHapticsSection = true,
            showFlagHapticsSection = false,
            showAppUpdatesOption = true,
            showDailyWinsSelector = true,
            dailyWinsGames = settings.dailyWinsGames,
            onToggleDailyWinsGame = { game, enabled ->
                AppSettingsStore.setGameCountsForDailyWins(context, game, enabled)
            },
            onOpenAppUpdates = {
                showSettingsDialog = false
                showUpdateDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showUpdateDialog) {
        GitHubUpdateDialog(
            isOledMode = settings.isOledMode,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun GameSelectionCard(
    entry: GameMenuEntry,
    isOledMode: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onStreakClick: () -> Unit
) {
    val colors = rememberArcadeThemeColors(isOledMode)
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(entry.testTag)
            .clip(shape)
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, shape)
            .clickable(
                role = Role.Button,
                onClickLabel = "Play ${entry.title}",
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
        ) {
            Image(
                painter = painterResource(id = entry.imageRes),
                contentDescription = "${entry.title} tabletop preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            // Independent per-game Daily Streak badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                DailyStreakBadge(
                    streak = entry.dailyStreak,
                    isOledMode = true,
                    isDark = isDark,
                    onClick = onStreakClick,
                    modifier = Modifier.testTag("${entry.testTag}_streak_badge")
                )
            }

            // Status / Play badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (entry.isInProgress) Color(0xFF16A34A) else entry.accentColor.copy(alpha = 0.92f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (entry.isInProgress) "Resume" else "Play",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = entry.title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.subtitle,
                fontSize = 12.sp,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.statsSummary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF60A5FA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
