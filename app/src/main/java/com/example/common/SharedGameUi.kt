package com.example.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minesweeper.data.DailyStreak
import com.example.minesweeper.model.GameStatus
import com.example.minesweeper.ui.components.DailyStreakBadge
import com.example.minesweeper.ui.components.DigitalCounter
import com.example.minesweeper.ui.components.SmileyButton

data class ArcadeThemeColors(
    val screenBackground: Color,
    val hudBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val pillInactiveBg: Color,
    val pillInactiveText: Color,
    val pillActiveBg: Color,
    val dialogSurface: Color
)

@Composable
fun rememberArcadeThemeColors(isOledMode: Boolean): ArcadeThemeColors {
    val isDark = isSystemInDarkTheme()
    return ArcadeThemeColors(
        screenBackground = when {
            isOledMode -> Color.Black
            isDark -> Color(0xFF0F172A)
            else -> Color(0xFFE2E8F0)
        },
        hudBackground = when {
            isOledMode -> Color.Black
            isDark -> Color(0xFF1E293B)
            else -> Color(0xFFF1F5F9)
        },
        cardBackground = when {
            isOledMode -> Color(0xFF0A0A0A)
            isDark -> Color(0xFF1E293B)
            else -> Color.White
        },
        cardBorder = when {
            isOledMode -> Color(0xFF262626)
            isDark -> Color(0xFF334155)
            else -> Color(0xFFCBD5E1)
        },
        primaryText = if (isDark || isOledMode) Color.White else Color(0xFF0F172A),
        secondaryText = if (isOledMode) Color(0xFF94A3B8) else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
        pillInactiveBg = when {
            isOledMode -> Color(0xFF171717)
            isDark -> Color(0xFF334155)
            else -> Color(0xFFE2E8F0)
        },
        pillInactiveText = if (isDark || isOledMode) Color(0xFFCBD5E1) else Color(0xFF334155),
        pillActiveBg = if (isDark || isOledMode) Color(0xFF2563EB) else Color(0xFF1D4ED8),
        dialogSurface = when {
            isOledMode -> Color(0xFF121212)
            isDark -> Color(0xFF1E293B)
            else -> Color.White
        }
    )
}

/**
 * Minesweeper-style top HUD shared by Solitaire, FreeCell, and Sudoku:
 * - Top row: Left LED counter, center Smiley reset button, right LED timer
 * - Action row: Back to menu, game title, Daily Streak badge, Sound toggle, OLED toggle, Stats
 */
@Composable
fun ArcadeGameHUD(
    title: String,
    leftCounterValue: Int,
    leftCounterTag: String,
    timerSeconds: Int,
    status: GameStatus,
    isSoundEnabled: Boolean,
    isOledMode: Boolean,
    dailyStreak: DailyStreak,
    onBackClick: () -> Unit,
    onResetClick: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleOledMode: () -> Unit,
    onOpenDailyStreak: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
    subRowContent: (@Composable () -> Unit)? = null
) {
    val colors = rememberArcadeThemeColors(isOledMode)
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.hudBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Navigation + Quick settings bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("back_to_home_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Game Selection",
                        tint = colors.primaryText
                    )
                }

                Text(
                    text = title,
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DailyStreakBadge(
                    streak = dailyStreak,
                    isOledMode = isOledMode,
                    isDark = isDark,
                    onClick = onOpenDailyStreak
                )

                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("sound_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = if (isSoundEnabled) "Mute sound" else "Enable sound",
                        tint = colors.primaryText
                    )
                }

                IconButton(
                    onClick = onToggleOledMode,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("oled_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = if (isOledMode) "Disable OLED pure black mode" else "Enable OLED pure black mode",
                        tint = if (isOledMode) Color(0xFF60A5FA) else colors.primaryText
                    )
                }

                IconButton(
                    onClick = onOpenStats,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Game statistics",
                        tint = colors.primaryText
                    )
                }
            }
        }

        // Signature Minesweeper LED counters + Smiley reset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DigitalCounter(
                value = leftCounterValue,
                testTag = leftCounterTag
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

        if (subRowContent != null) {
            Spacer(modifier = Modifier.height(4.dp))
            subRowContent()
        }
    }
}

@Composable
fun ArcadePillButton(
    label: String,
    selected: Boolean,
    isOledMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color? = null,
    testTag: String? = null
) {
    val colors = rememberArcadeThemeColors(isOledMode)
    val bg = when {
        !enabled -> colors.pillInactiveBg.copy(alpha = 0.45f)
        selected -> accentColor ?: colors.pillActiveBg
        else -> colors.pillInactiveBg
    }
    val textColor = when {
        !enabled -> colors.pillInactiveText.copy(alpha = 0.4f)
        selected -> Color.White
        else -> colors.pillInactiveText
    }

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(bg)
            .then(
                if (isOledMode && !selected) {
                    Modifier.border(1.dp, Color(0xFF262626), RoundedCornerShape(15.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun ArcadeVictoryDialog(
    title: String,
    subtitle: String,
    isNewBest: Boolean,
    isOledMode: Boolean,
    onPlayAgain: () -> Unit,
    onBackToHome: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberArcadeThemeColors(isOledMode)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Text(text = "😎", fontSize = 24.sp)
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.primaryText,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("arcade_victory_dialog"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isNewBest) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🏆 NEW BEST TIME! 🏆",
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier.testTag("victory_home_button")
                ) {
                    Text("Games")
                }
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.testTag("victory_play_again_button")
                ) {
                    Text("Play Again")
                }
            }
        },
        dismissButton = null
    )
}
