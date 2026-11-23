package com.example.minesweeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minesweeper.data.DailyStreak
import com.example.minesweeper.data.DifficultyStats
import com.example.minesweeper.model.DifficultyType

@Composable
fun StatsDialog(
    stats: Map<DifficultyType, DifficultyStats>,
    dailyStreak: DailyStreak? = null,
    onResetStats: (DifficultyType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DifficultyType.BEGINNER) }
    val currentStats = stats[selectedTab] ?: DifficultyStats()

    val tabs = listOf(
        DifficultyType.BEGINNER to "Beginner",
        DifficultyType.INTERMEDIATE to "Intermediate",
        DifficultyType.EXPERT to "Expert"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Statistics",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stats_dialog")
            ) {
                // Daily Streak Card
                dailyStreak?.let { ds ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("daily_streak_stats_card"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🔥", fontSize = 18.sp)
                                    Text(
                                        text = "Daily Streak",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${ds.currentStreak} ${if (ds.currentStreak == 1) "day" else "days"}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "· Best: ${ds.longestStreak}d",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (ds.isWinAwardedToday) "Today completed! Keep it up tomorrow." else "Win a board today to extend your streak!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
                ) {
                    for (tab in tabs) {
                        Tab(
                            selected = selectedTab == tab.first,
                            onClick = { selectedTab = tab.first }
                        ) {
                            Text(
                                text = tab.second,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == tab.first) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatRow("Games Played", currentStats.played.toString())
                        StatRow("Games Won", currentStats.won.toString())
                        StatRow("Win Rate", "${currentStats.winPercentage}%")
                        val bestTimeStr = if (currentStats.bestTimeSeconds != null) {
                            "${currentStats.bestTimeSeconds}s"
                        } else {
                            "—"
                        }
                        StatRow("Best Time", bestTimeStr)
                        StatRow("Current Streak", currentStats.currentStreak.toString())
                        StatRow("Longest Win Streak", currentStats.maxStreak.toString())
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onResetStats(selectedTab) },
                        modifier = Modifier.testTag("reset_stats_button")
                    ) {
                        Text(
                            text = "Reset This Category",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_stats_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
