package com.example.minesweeper.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minesweeper.data.DailyStreak
import com.example.minesweeper.model.Difficulty
import com.example.minesweeper.model.DifficultyType
import com.example.minesweeper.model.GameStatus
import com.example.minesweeper.model.InteractionMode

@Composable
fun GameHUD(
    status: GameStatus,
    remainingMines: Int,
    timerSeconds: Int,
    currentDifficulty: Difficulty,
    interactionMode: InteractionMode,
    isSoundEnabled: Boolean,
    isOledMode: Boolean = false,
    dailyStreak: DailyStreak = DailyStreak(),
    onResetClick: () -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onToggleExpertOrientation: () -> Unit,
    onToggleInteractionMode: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleOledMode: () -> Unit = {},
    onOpenDailyStreak: () -> Unit = {},
    onOpenHapticSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onBackToHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val hudBackground = when {
        isOledMode -> Color.Black
        isDark -> Color(0xFF1E293B)
        else -> Color(0xFFF1F5F9)
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Compact single-row HUD in landscape mode to maximize vertical space for the board
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(hudBackground)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button, Mode switch, Daily Streak badge, and quick settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onBackToHome != null) {
                    IconButton(
                        onClick = onBackToHome,
                        modifier = Modifier.size(32.dp).testTag("back_to_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Game Selection",
                            modifier = Modifier.size(18.dp),
                            tint = if (isDark || isOledMode) Color.White else Color(0xFF1E293B)
                        )
                    }
                }
                ModeToggleButton(
                    currentMode = interactionMode,
                    onToggle = onToggleInteractionMode
                )

                DailyStreakBadge(
                    streak = dailyStreak,
                    isOledMode = isOledMode,
                    isDark = isDark,
                    onClick = onOpenDailyStreak
                )

                IconButton(
                    onClick = onOpenHapticSettings,
                    modifier = Modifier.size(34.dp).testTag("haptic_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Game Settings",
                        modifier = Modifier.size(20.dp),
                        tint = if (isDark || isOledMode) Color.White else Color(0xFF1E293B)
                    )
                }
                IconButton(
                    onClick = onOpenStats,
                    modifier = Modifier.size(34.dp).testTag("stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Game statistics",
                        modifier = Modifier.size(20.dp),
                        tint = if (isDark || isOledMode) Color.White else Color(0xFF1E293B)
                    )
                }
            }

            // Center: Mines, Smiley, Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DigitalCounter(
                    value = remainingMines,
                    testTag = "counter_mines"
                )
                SmileyButton(
                    status = status,
                    onClick = onResetClick
                )
                DigitalCounter(
                    value = timerSeconds,
                    testTag = "counter_timer"
                )
            }

            // Right: Current difficulty indicator
            Text(
                text = currentDifficulty.name,
                color = if (isDark || isOledMode) Color(0xFF94A3B8) else Color(0xFF475569),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        // Standard Portrait Column Layout
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(hudBackground)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Digits & Smiley & Quick Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mines counter
                DigitalCounter(
                    value = remainingMines,
                    testTag = "counter_mines"
                )

                // Center Smiley Button
                SmileyButton(
                    status = status,
                    onClick = onResetClick
                )

                // Timer counter
                DigitalCounter(
                    value = timerSeconds,
                    testTag = "counter_timer"
                )
            }

            // Secondary Action Row: Mode Switcher (Dig/Flag), Daily Streak & Quick Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onBackToHome != null) {
                        IconButton(
                            onClick = onBackToHome,
                            modifier = Modifier.size(34.dp).testTag("back_to_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Game Selection",
                                tint = if (isDark || isOledMode) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }

                    // Interaction Mode Toggle (Dig ⛏️ / Flag 🚩)
                    ModeToggleButton(
                        currentMode = interactionMode,
                        onToggle = onToggleInteractionMode
                    )

                    // Daily Streak Counter Badge
                    DailyStreakBadge(
                        streak = dailyStreak,
                        isOledMode = isOledMode,
                        isDark = isDark,
                        onClick = onOpenDailyStreak
                    )
                }

                // Settings & Stats actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = currentDifficulty.name,
                        color = if (isDark || isOledMode) Color(0xFF94A3B8) else Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = onOpenHapticSettings,
                        modifier = Modifier.testTag("haptic_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Game Settings",
                            tint = if (isDark || isOledMode) Color.White else Color(0xFF1E293B)
                        )
                    }

                    IconButton(
                        onClick = onOpenStats,
                        modifier = Modifier.testTag("stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Game statistics",
                            tint = if (isDark || isOledMode) Color.White else Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyStreakBadge(
    streak: DailyStreak,
    isOledMode: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val count = streak.currentStreak
    val isAwarded = streak.isWinAwardedToday

    Box(
        modifier = modifier
            .testTag("daily_streak_badge")
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isAwarded -> Color(0xFFE65100).copy(alpha = 0.25f)
                    isOledMode -> Color(0xFF1E1E1E)
                    isDark -> Color(0xFF334155)
                    else -> Color(0xFFE2E8F0)
                }
            )
            .clickable(
                role = Role.Button,
                onClickLabel = "View Daily Streak: $count days",
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "🔥",
                fontSize = 13.sp
            )
            Text(
                text = "${count}d",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAwarded) Color(0xFFFF9800) else if (isDark || isOledMode) Color.White else Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun DifficultyPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val isDark = isSystemInDarkTheme()
    val activeBg = if (isDark) Color(0xFF2563EB) else Color(0xFF1D4ED8)
    val inactiveBg = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val activeText = Color.White
    val inactiveText = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .testTag(testTag)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) activeBg else inactiveBg)
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) activeText else inactiveText,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ModeToggleButton(
    currentMode: InteractionMode,
    onToggle: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isDig = currentMode == InteractionMode.DIG

    Row(
        modifier = Modifier
            .testTag("mode_toggle_button")
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Toggle Dig or Flag mode",
                onClick = onToggle
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDig) Color(0xFF2563EB) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⛏️ DIG",
                color = if (isDig) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                fontSize = 12.sp,
                fontWeight = if (isDig) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (!isDig) Color(0xFFDC2626) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🚩 FLAG",
                color = if (!isDig) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                fontSize = 12.sp,
                fontWeight = if (!isDig) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
