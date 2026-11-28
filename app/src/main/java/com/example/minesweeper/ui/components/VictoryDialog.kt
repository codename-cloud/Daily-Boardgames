package com.example.minesweeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.GameStatus
import com.example.minesweeper.model.Difficulty

@Composable
fun GameOverDialog(
    status: GameStatus,
    timerSeconds: Int,
    difficulty: Difficulty,
    isNewBestTime: Boolean,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    if (status != GameStatus.WON && status != GameStatus.LOST) return

    val isWon = status == GameStatus.WON
    val title = if (isWon) "🎉 Victory!" else "💥 Game Over"
    val icon = if (isWon) "😎" else "💣"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Text(text = icon, fontSize = 24.sp)
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("game_over_dialog"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isWon) {
                    Text(
                        text = "You cleared the ${difficulty.name} board in $timerSeconds seconds!",
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isNewBestTime) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🏆 NEW RECORD! 🏆",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = "You triggered a mine! Better luck next time.",
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
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_dialog_button")
                ) {
                    Text("View Board")
                }
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.testTag("play_again_button")
                ) {
                    Text("Play Again")
                }
            }
        },
        dismissButton = null
    )
}
