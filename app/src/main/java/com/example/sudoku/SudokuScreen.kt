package com.example.sudoku

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.ArcadeGameHUD
import com.example.common.ArcadePillButton
import com.example.common.ArcadeVictoryDialog
import com.example.common.rememberArcadeThemeColors
import com.example.minesweeper.ui.components.DailyStreakDialog
import com.example.minesweeper.ui.components.HapticSettingsDialog
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun SudokuScreen(
    viewModel: SudokuViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    LaunchedEffect(Unit) {
        viewModel.refreshSharedSettings()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = rememberArcadeThemeColors(uiState.isOledMode)
    val isDark = isSystemInDarkTheme()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.screenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
        ) {
            ArcadeGameHUD(
                title = "Sudoku",
                leftCounterValue = uiState.remainingEmptyCells,
                leftCounterTag = "sudoku_remaining_counter",
                timerSeconds = uiState.timerSeconds,
                status = uiState.status,
                isOledMode = uiState.isOledMode,
                dailyStreak = uiState.dailyStreak,
                onBackClick = onBackToHome,
                onResetClick = { viewModel.startNewGame() },
                onOpenSettings = { viewModel.showSettingsDialog(true) },
                onOpenDailyStreak = { viewModel.showDailyStreakDialog(true) },
                onOpenStats = { viewModel.showStatsDialog(true) },
                landscapeActionsContent = {
                    Text(
                        text = uiState.difficulty.label,
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Mistakes: ${uiState.mistakes}",
                        color = if (uiState.mistakes > 0) Color(0xFFF87171) else colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.testTag("sudoku_mistakes_text")
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Difficulty: ${uiState.difficulty.label}",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("sudoku_difficulty_label")
                    )

                    Text(
                        text = "Mistakes: ${uiState.mistakes}",
                        color = if (uiState.mistakes > 0) Color(0xFFF87171) else colors.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.testTag("sudoku_mistakes_text")
                    )
                }
            }

            // 9x9 Sudoku Grid (Minesweeper square-tile styling + OLED pure black support)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.board.isNotEmpty()) {
                    SudokuBoardView(
                        board = uiState.board,
                        selectedCell = uiState.selectedCell,
                        isOledMode = uiState.isOledMode,
                        isDark = isDark,
                        onCellClick = { r, c -> viewModel.selectCell(r, c) }
                    )
                }
            }

            // Tools Row: Undo, Erase, Notes, Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                ArcadePillButton(
                    label = "Undo",
                    selected = false,
                    enabled = uiState.canUndo,
                    isOledMode = uiState.isOledMode,
                    onClick = { viewModel.undo() },
                    modifier = Modifier.weight(1f),
                    testTag = "sudoku_undo_button"
                )
                ArcadePillButton(
                    label = "Erase",
                    selected = false,
                    isOledMode = uiState.isOledMode,
                    onClick = { viewModel.eraseCell() },
                    modifier = Modifier.weight(1f),
                    testTag = "sudoku_erase_button"
                )
                ArcadePillButton(
                    label = if (uiState.isNotesMode) "Notes ON" else "Notes",
                    selected = uiState.isNotesMode,
                    isOledMode = uiState.isOledMode,
                    onClick = { viewModel.toggleNotesMode() },
                    modifier = Modifier.weight(1f),
                    testTag = "sudoku_notes_button"
                )
                ArcadePillButton(
                    label = "Hint",
                    selected = false,
                    isOledMode = uiState.isOledMode,
                    onClick = { viewModel.useHint() },
                    modifier = Modifier.weight(1f),
                    testTag = "sudoku_hint_button"
                )
            }

            // 1..9 Number Pad
            val digitCounts = remember(uiState.board) {
                val counts = IntArray(10)
                for (row in uiState.board) {
                    for (cell in row) {
                        if (cell.value in 1..9 && !cell.isError) counts[cell.value]++
                    }
                }
                counts
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (digit in 1..9) {
                    val remaining = (9 - digitCounts[digit]).coerceAtLeast(0)
                    val isDone = remaining == 0
                    val keyBg = when {
                        isDone -> colors.pillInactiveBg.copy(alpha = 0.35f)
                        uiState.isOledMode -> Color(0xFF111827)
                        isDark -> Color(0xFF1E293B)
                        else -> Color.White
                    }
                    val keyBorder = when {
                        isDone -> Color.Transparent
                        uiState.isOledMode -> Color(0xFF334155)
                        isDark -> Color(0xFF475569)
                        else -> Color(0xFF94A3B8)
                    }
                    val keyTextColor = when {
                        isDone -> colors.secondaryText.copy(alpha = 0.35f)
                        uiState.isOledMode || isDark -> Color(0xFF60A5FA)
                        else -> Color(0xFF1D4ED8)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(keyBg)
                            .border(1.dp, keyBorder, RoundedCornerShape(8.dp))
                            .clickable(
                                enabled = !isDone,
                                role = Role.Button,
                                onClick = { viewModel.enterDigit(digit) }
                            )
                            .testTag("sudoku_num_$digit"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            color = keyTextColor,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = remaining.toString(),
                            color = colors.secondaryText.copy(alpha = if (isDone) 0.25f else 0.75f),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDailyStreakDialog) {
        DailyStreakDialog(
            streak = uiState.dailyStreak,
            isOledMode = uiState.isOledMode,
            gameLabel = "Sudoku",
            onDismiss = { viewModel.showDailyStreakDialog(false) }
        )
    } else if (uiState.showVictoryDialog) {
        ArcadeVictoryDialog(
            title = "Sudoku Solved!",
            subtitle = "Completed ${uiState.difficulty.label} in ${uiState.timerSeconds}s with ${uiState.mistakes} mistakes.",
            isNewBest = uiState.isNewBestTime,
            isOledMode = uiState.isOledMode,
            onPlayAgain = { viewModel.startNewGame() },
            onBackToHome = {
                viewModel.dismissVictoryDialog()
                onBackToHome()
            },
            onDismiss = { viewModel.dismissVictoryDialog() }
        )
    }

    if (uiState.showSettingsDialog) {
        HapticSettingsDialog(
            isHapticsEnabled = uiState.isHapticsEnabled,
            isOledMode = uiState.isOledMode,
            isSoundEnabled = uiState.isSoundEnabled,
            onToggleSound = { viewModel.setSoundEnabled(it) },
            onToggleMasterHaptics = { viewModel.setHapticsEnabled(it) },
            onToggleOledMode = { viewModel.setOledMode(it) },
            showFlagHapticsSection = false,
            onDismiss = { viewModel.showSettingsDialog(false) },
            gameOptionsContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Difficulty",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.primaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SudokuDifficulty.entries.forEach { diff ->
                            ArcadePillButton(
                                label = diff.shortLabel,
                                selected = uiState.difficulty == diff,
                                isOledMode = uiState.isOledMode,
                                onClick = { viewModel.startNewGame(diff) },
                                modifier = Modifier.weight(1f),
                                testTag = "sudoku_diff_${diff.name.lowercase()}"
                            )
                        }
                    }
                }
            }
        )
    }

    if (uiState.showStatsDialog) {
        SudokuStatsDialog(
            stats = uiState.stats,
            isOledMode = uiState.isOledMode,
            onReset = { viewModel.resetStats(it) },
            onDismiss = { viewModel.showStatsDialog(false) }
        )
    }
}

@Composable
private fun SudokuBoardView(
    board: List<List<SudokuCell>>,
    selectedCell: Pair<Int, Int>?,
    isOledMode: Boolean,
    isDark: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    val selectedValue = selectedCell?.let { (r, c) -> board[r][c].value } ?: 0
    val thinLineColor = when {
        isOledMode -> Color(0xFF2D3748)
        isDark -> Color(0xFF475569)
        else -> Color(0xFF94A3B8)
    }
    val thickLineColor = when {
        isOledMode -> Color(0xFF64748B)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF334155)
    }
    val boardShape = RoundedCornerShape(10.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sudoku_board"),
        contentAlignment = Alignment.Center
    ) {
        val boardSide = minOf(maxWidth, maxHeight)

        Box(
            modifier = Modifier
                .size(boardSide)
                .clip(boardShape)
                .background(if (isOledMode) Color.Black else if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                .drawWithContent {
                    drawContent()
                    val cellPx = size.width / 9f
                    for (i in 1 until 9) {
                        val pos = cellPx * i
                        val isThick = i % 3 == 0
                        val stroke = if (isThick) 2.dp.toPx() else 0.8.dp.toPx()
                        val color = if (isThick) thickLineColor else thinLineColor
                        drawLine(color = color, start = Offset(pos, 0f), end = Offset(pos, size.height), strokeWidth = stroke)
                        drawLine(color = color, start = Offset(0f, pos), end = Offset(size.width, pos), strokeWidth = stroke)
                    }
                }
                .border(2.dp, thickLineColor, boardShape)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 9) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (c in 0 until 9) {
                            val cell = board[r][c]
                            val isSelected = selectedCell == (r to c)
                            val isPeer = selectedCell != null && !isSelected && (
                                selectedCell.first == r ||
                                    selectedCell.second == c ||
                                    (selectedCell.first / 3 == r / 3 && selectedCell.second / 3 == c / 3)
                                )
                            val isSameDigit = selectedValue != 0 && cell.value == selectedValue && !isSelected
                            val isAltBox = ((r / 3) + (c / 3)) % 2 == 1

                            val cellBg = when {
                                isSelected -> if (isOledMode) Color(0xFF1D4ED8).copy(alpha = 0.55f) else if (isDark) Color(0xFF1D4ED8).copy(alpha = 0.45f) else Color(0xFF93C5FD)
                                cell.isError -> if (isOledMode) Color(0xFF7F1D1D).copy(alpha = 0.7f) else Color(0xFFEF4444).copy(alpha = 0.28f)
                                isSameDigit -> if (isOledMode) Color(0xFF1E3A8A).copy(alpha = 0.55f) else if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFBFDBFE)
                                isPeer -> if (isOledMode) Color(0xFF0F172A) else if (isDark) Color(0xFF283548) else Color(0xFFDFE7F1)
                                isAltBox -> when {
                                    isOledMode -> Color(0xFF0D0F14)
                                    isDark -> Color(0xFF253042)
                                    else -> Color(0xFFE9EFF6)
                                }
                                else -> when {
                                    isOledMode -> Color.Black
                                    isDark -> Color(0xFF1E293B)
                                    else -> Color(0xFFF8FAFC)
                                }
                            }

                            val textColor = when {
                                cell.isError -> Color(0xFFEF4444)
                                cell.isGiven -> if (isOledMode || isDark) Color.White else Color(0xFF0F172A)
                                else -> if (isOledMode || isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                            }

                            val density = LocalDensity.current
                            val cellSide = boardSide / 9f
                            val subCellSide = (cellSide - 3.dp) / 3f
                            val digitFontSize = (cellSide.value * 0.54f).coerceIn(13f, 26f).sp
                            val noteTextDp = (subCellSide * 0.78f).coerceIn(7.dp, 12.dp)
                            val noteFontSize = with(density) { noteTextDp.toSp() }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellBg)
                                    .clickable { onCellClick(r, c) }
                                    .testTag("sudoku_cell_${r}_${c}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.value != 0) {
                                    Text(
                                        text = cell.value.toString(),
                                        color = textColor,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = if (cell.isGiven) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = digitFontSize,
                                        textAlign = TextAlign.Center
                                    )
                                } else if (cell.notes.isNotEmpty()) {
                                    val noteColor = if (isOledMode || isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(1.5.dp)
                                    ) {
                                        for (nr in 0 until 3) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                            ) {
                                                for (nc in 0 until 3) {
                                                    val n = nr * 3 + nc + 1
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (n in cell.notes) {
                                                            Text(
                                                                text = n.toString(),
                                                                color = noteColor,
                                                                style = TextStyle(
                                                                    fontSize = noteFontSize,
                                                                    lineHeight = noteFontSize,
                                                                    fontFamily = JetBrainsMonoFontFamily,
                                                                    fontWeight = FontWeight.Medium,
                                                                    textAlign = TextAlign.Center,
                                                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                                    lineHeightStyle = LineHeightStyle(
                                                                        alignment = LineHeightStyle.Alignment.Center,
                                                                        trim = LineHeightStyle.Trim.Both
                                                                    )
                                                                ),
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.wrapContentSize(unbounded = true)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuStatsDialog(
    stats: Map<SudokuDifficulty, SudokuStats>,
    isOledMode: Boolean,
    onReset: (SudokuDifficulty) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberArcadeThemeColors(isOledMode)
    var selectedTab by remember { mutableStateOf(SudokuDifficulty.EASY) }
    val current = stats[selectedTab] ?: SudokuStats()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogSurface,
        title = {
            Text(
                text = "Sudoku Statistics",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.primaryText
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = SudokuDifficulty.entries.indexOf(selectedTab),
                    containerColor = Color.Transparent
                ) {
                    SudokuDifficulty.entries.forEach { diff ->
                        Tab(
                            selected = selectedTab == diff,
                            onClick = { selectedTab = diff }
                        ) {
                            Text(
                                text = diff.shortLabel,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == diff) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == diff) colors.primaryText else colors.secondaryText,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOledMode) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SudokuStatRow("Games Played", current.played.toString(), colors.secondaryText, colors.primaryText)
                        SudokuStatRow("Games Solved", current.won.toString(), colors.secondaryText, colors.primaryText)
                        SudokuStatRow("Win Rate", "${current.winPercentage}%", colors.secondaryText, colors.primaryText)
                        SudokuStatRow("Best Time", current.bestTimeSeconds?.let { "${it}s" } ?: "—", colors.secondaryText, colors.primaryText)
                        SudokuStatRow("Current Streak", current.currentStreak.toString(), colors.secondaryText, colors.primaryText)
                        SudokuStatRow("Best Streak", current.maxStreak.toString(), colors.secondaryText, colors.primaryText)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onReset(selectedTab) }) {
                        Text("Reset Category", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SudokuStatRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = labelColor)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
