package com.example.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmileyButton(
    status: GameStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val smileyText = when (status) {
        GameStatus.READY, GameStatus.RUNNING -> "🙂"
        GameStatus.WON -> "😎"
        GameStatus.LOST -> "😵"
    }

    val contentDesc = when (status) {
        GameStatus.READY, GameStatus.RUNNING -> "Reset Game - In Progress"
        GameStatus.WON -> "Reset Game - Won"
        GameStatus.LOST -> "Reset Game - Game Over"
    }

    Box(
        modifier = modifier
            .testTag("smiley_button")
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDesc,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = smileyText,
            fontSize = 30.sp
        )
    }
}
