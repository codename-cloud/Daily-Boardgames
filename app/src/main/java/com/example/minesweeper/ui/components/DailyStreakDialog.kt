package com.example.minesweeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minesweeper.data.DailyStreak

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
fun DailyStreakDialog(
    streak: DailyStreak,
    isOledMode: Boolean = false,
    gameLabel: String? = null,
    onDismiss: () -> Unit
) {
    val count = streak.currentStreak
    val copy = resolveDailyStreakDialogCopy(streak)
    val titleMessage = copy.title
    val subtitleMessage = copy.subtitle

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isOledMode) Color(0xFF121212) else MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_streak_dialog"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Flame Icon Badge with high-contrast dark background
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
                        .size(80.dp)
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
                        fontSize = 42.sp
                    )
                }

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
                    text = titleMessage,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = if (isOledMode) Color.White else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitleMessage,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = if (isOledMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats card
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                fontSize = 18.sp,
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOledMode) Color(0xFFFFD54F) else Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
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
                    text = copy.buttonLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    )
}
