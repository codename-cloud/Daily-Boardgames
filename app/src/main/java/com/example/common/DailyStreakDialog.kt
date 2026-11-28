package com.example.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal data class DailyStreakDialogCopy(
    val title: String,
    val subtitle: String,
    val buttonLabel: String
)

internal fun resolveDailyStreakDialogCopy(streak: DailyStreak): DailyStreakDialogCopy {
    val count = streak.currentStreak
    val isAwardedToday = streak.isWinAwardedToday

    return when {
        !isAwardedToday && count == 0 -> DailyStreakDialogCopy(
            title = "Start a Streak!",
            subtitle = "Win a board today to light the flame and start your daily streak!",
            buttonLabel = "Let's Play!"
        )
        !isAwardedToday -> {
            val dayLabel = if (count == 1) "day" else "days"
            DailyStreakDialogCopy(
                title = "$count-Day Streak!",
                subtitle = "You've won a board $count $dayLabel in a row! Win a board today to keep the flame alive!",
                buttonLabel = "Let's Play!"
            )
        }
        count <= 1 -> DailyStreakDialogCopy(
            title = "First Win of the Day!",
            subtitle = "You completed your first board today! Win a game every day to keep your streak burning.",
            buttonLabel = "Keep It Up!"
        )
        else -> DailyStreakDialogCopy(
            title = "$count-Day Streak!",
            subtitle = "You've won a board $count days in a row! Come back tomorrow to keep the flame alive.",
            buttonLabel = "Keep It Up!"
        )
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
fun DailyStreakDialog(
    streak: DailyStreak,
    isOledMode: Boolean = false,
    gameLabel: String? = null,
    onDismiss: () -> Unit
) {
    val copy = resolveDailyStreakDialogCopy(streak)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val containerColor = if (isOledMode) Color(0xFF121212) else MaterialTheme.colorScheme.surface

    if (isLandscape) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = containerColor,
                tonalElevation = if (isOledMode) 0.dp else 6.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("daily_streak_dialog")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1.05f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StreakFlameBadge(
                            isOledMode = isOledMode,
                            badgeSize = 58.dp,
                            emojiSize = 30.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (gameLabel != null) {
                            Text(
                                text = gameLabel.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFFFF9800),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = copy.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            textAlign = TextAlign.Center,
                            color = if (isOledMode) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = copy.subtitle,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = if (isOledMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StreakStatsCard(
                            streak = streak,
                            isOledMode = isOledMode,
                            compact = true
                        )
                        StreakConfirmButton(
                            label = copy.buttonLabel,
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = containerColor,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .testTag("daily_streak_dialog"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    StreakFlameBadge(
                        isOledMode = isOledMode,
                        badgeSize = 80.dp,
                        emojiSize = 42.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (gameLabel != null) {
                        Text(
                            text = gameLabel.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFFFF9800),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = copy.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        color = if (isOledMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = copy.subtitle,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = if (isOledMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    StreakStatsCard(
                        streak = streak,
                        isOledMode = isOledMode,
                        compact = false
                    )
                }
            },
            confirmButton = {
                StreakConfirmButton(
                    label = copy.buttonLabel,
                    onClick = onDismiss
                )
            }
        )
    }
}

@Composable
private fun StreakFlameBadge(
    isOledMode: Boolean,
    badgeSize: Dp,
    emojiSize: TextUnit
) {
    val badgeGradient = if (isOledMode) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF262626),
                Color(0xFF121212)
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF2D3748),
                Color(0xFF1A202C)
            )
        )
    }

    Box(
        modifier = Modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(badgeGradient)
            .border(
                width = 2.dp,
                color = Color(0xFFFF9800).copy(alpha = 0.6f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🔥",
            fontSize = emojiSize
        )
    }
}

@Composable
private fun StreakStatsCard(
    streak: DailyStreak,
    isOledMode: Boolean,
    compact: Boolean
) {
    val count = streak.currentStreak
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOledMode) Color(0xFF1E1E1E) else Color(0xFFFFF3E0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 12.dp else 16.dp,
                    vertical = if (compact) 10.dp else 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Current Streak",
                    fontSize = 11.sp,
                    color = if (isOledMode) Color(0xFFAAAAAA) else Color(0xFFE65100),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "🔥 $count ${if (count == 1) "day" else "days"}",
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOledMode) Color(0xFFFFB74D) else Color(0xFFBF360C)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Longest Streak",
                    fontSize = 11.sp,
                    color = if (isOledMode) Color(0xFFAAAAAA) else Color(0xFF795548),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "🏆 ${streak.longestStreak} ${if (streak.longestStreak == 1) "day" else "days"}",
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOledMode) Color(0xFFFFD54F) else Color(0xFF5D4037)
                )
            }
        }
    }
}

@Composable
private fun StreakConfirmButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE65100),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("close_daily_streak_button")
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
