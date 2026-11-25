package com.example.minesweeper.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minesweeper.model.Cell
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun MinesweeperBoard(
    board: List<List<Cell>>,
    fillScreen: Boolean,
    isOledMode: Boolean = false,
    onCellClick: (row: Int, col: Int) -> Unit,
    onCellLongClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (board.isEmpty()) return
    val rows = board.size
    val cols = board[0].size

    val isDark = LocalIsDarkTheme.current
    val effectiveOled = isDark && isOledMode
    // Grid border color matching both perimeter and inner cell borders
    val gridBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFF94A3B8)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("minesweeper_board"),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // Cells are always true squares, dynamically sized to fit available screen space
        val cellSide = minOf(totalWidth / cols, totalHeight / rows)

        Column(
            modifier = Modifier
                .border(0.5.dp, gridBorderColor)
                .background(if (effectiveOled) Color.Black else if (isDark) Color(0xFF0F172A) else Color(0xFF94A3B8))
        ) {
            for (r in 0 until rows) {
                Row {
                    for (c in 0 until cols) {
                        val cell = board[r][c]
                        CellView(
                            cell = cell,
                            side = cellSide,
                            gridBorderColor = gridBorderColor,
                            isOledMode = isOledMode,
                            onClick = { onCellClick(r, c) },
                            onLongClick = { onCellLongClick(r, c) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellView(
    cell: Cell,
    side: Dp,
    gridBorderColor: Color,
    isOledMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val effectiveOled = isDark && isOledMode

    // Base background colors: In OLED mode, revealed cells are pure black (#000000)
    val backgroundColor = when {
        cell.isExploded -> Color(0xFFEF4444) // Bright red for exploded mine
        cell.isRevealed -> if (effectiveOled) Color.Black else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        else -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    }

    // Calculate font size responsive to cell dimension
    val fontSizeSp = (side.value * 0.58f).coerceIn(10f, 30f).sp

    Box(
        modifier = Modifier
            .size(side)
            .testTag("cell_${cell.row}_${cell.col}")
            .background(backgroundColor)
            .border(0.5.dp, gridBorderColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            cell.isFlagged -> {
                Text(
                    text = if (cell.isFalseFlag) "❌" else "🚩",
                    fontSize = fontSizeSp
                )
            }
            cell.isRevealed && cell.isMine -> {
                Text(
                    text = "💣",
                    fontSize = fontSizeSp
                )
            }
            cell.isRevealed && cell.adjacentMines > 0 -> {
                val numberColor = getNumberColor(cell.adjacentMines, isDark)
                Text(
                    text = cell.adjacentMines.toString(),
                    color = numberColor,
                    fontSize = fontSizeSp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getNumberColor(count: Int, isDark: Boolean): Color {
    return when (count) {
        1 -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB) // Blue
        2 -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A) // Green
        3 -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626) // Red
        4 -> if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED) // Purple
        5 -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706) // Amber/Maroon
        6 -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488) // Teal
        7 -> if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B) // Dark/Light Slate
        8 -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B) // Gray
        else -> Color.Gray
    }
}
