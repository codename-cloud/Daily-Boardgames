package com.example.minesweeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🎮 How to Play",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HelpSection(
                    title = "🎯 Objective",
                    content = "Uncover all cells on the board that do NOT contain mines. If you uncover a mine, boom!"
                )

                Spacer(modifier = Modifier.height(12.dp))

                HelpSection(
                    title = "🔢 The Numbers",
                    content = "Each revealed number tells you exactly how many mines are hidden in the 8 adjacent neighboring cells."
                )

                Spacer(modifier = Modifier.height(12.dp))

                HelpSection(
                    title = "⛏️ Dig vs 🚩 Flag Mode",
                    content = "• Tap the DIG / FLAG pill to switch active mode.\n• You can also LONG-PRESS any unrevealed cell to toggle a flag without switching modes!"
                )

                Spacer(modifier = Modifier.height(12.dp))

                HelpSection(
                    title = "⚡ Chording (Fast Play)",
                    content = "Once you have flagged the correct number of adjacent mines around a revealed number, TAP the number to instantly clear all other adjacent unflagged cells!"
                )

                Spacer(modifier = Modifier.height(12.dp))

                HelpSection(
                    title = "📱 Screen Adaptive",
                    content = "The board automatically resizes and scales to fit your screen perfectly on Beginner (9×9), Intermediate (16×16), and Expert (30×16 / 16×30) without any scrolling required!"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It!", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun HelpSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}
