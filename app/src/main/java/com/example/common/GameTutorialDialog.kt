package com.example.common

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cards.EmptyCardSlot
import com.example.cards.PlayingCard
import com.example.cards.PlayingCardView
import com.example.cards.Rank
import com.example.cards.Suit
import com.example.home.GameDestination
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.OutfitFontFamily

data class TutorialStep(
    val title: String,
    val description: String,
    val visualType: TutorialVisualType
)

enum class TutorialVisualType {
    MINE_UNCOVER,
    MINE_NUMBERS,
    MINE_FLAG,
    MINE_CHORD,
    SOL_FOUNDATIONS,
    SOL_CASCADE,
    SOL_STOCK,
    SOL_CONTROLS,
    FC_TOP_CELLS,
    FC_SUPERMOVE,
    FC_SPACE,
    FC_CONTROLS,
    SUDOKU_GRID,
    SUDOKU_PEERS,
    SUDOKU_NOTES,
    SUDOKU_TOOLS
}

internal fun tutorialStepsFor(game: GameDestination): List<TutorialStep> = when (game) {
    GameDestination.MINESWEEPER -> listOf(
        TutorialStep(
            title = "Uncover Safe Tiles",
            description = "Tap any covered square to reveal it. Clear all safe squares without hitting a mine to win—your first tap is always safe!",
            visualType = TutorialVisualType.MINE_UNCOVER
        ),
        TutorialStep(
            title = "Read the Numbers",
            description = "Every revealed number tells you how many mines are hidden in the 8 surrounding squares (including diagonals).",
            visualType = TutorialVisualType.MINE_NUMBERS
        ),
        TutorialStep(
            title = "Mark Mines with Flags",
            description = "Switch to 🚩 FLAG mode—or long-press any covered square in DIG mode—to plant a flag on a suspected mine.",
            visualType = TutorialVisualType.MINE_FLAG
        ),
        TutorialStep(
            title = "Chord & Guess-Free Play",
            description = "Once a number's mines are flagged, tap the number to clear its remaining safe neighbors automatically. Every board is solvable with pure logic!",
            visualType = TutorialVisualType.MINE_CHORD
        )
    )

    GameDestination.SOLITAIRE -> listOf(
        TutorialStep(
            title = "Build the 4 Foundations",
            description = "Move all 52 cards into the 4 top-right Ace piles by suit, stacking upward from Ace (A) through King (K).",
            visualType = TutorialVisualType.SOL_FOUNDATIONS
        ),
        TutorialStep(
            title = "Stack Alternating Colors",
            description = "On the 7 tableau columns, build descending sequences alternating red and black (e.g., 7♠ → 6♥ → 5♣). Only Kings can fill empty columns.",
            visualType = TutorialVisualType.SOL_CASCADE
        ),
        TutorialStep(
            title = "Draw from the Stock",
            description = "Tap the top-left Stock pile to flip cards into the Waste pile. You can switch between Draw 1 and Draw 3 anytime in Game Settings.",
            visualType = TutorialVisualType.SOL_STOCK
        ),
        TutorialStep(
            title = "Smart Moves & Auto-Finish",
            description = "Tap a card to select it, then tap a destination—or tap Auto once all cards are face up to finish cleanly. Undo, Hint, and Replay are always ready.",
            visualType = TutorialVisualType.SOL_CONTROLS
        )
    )

    GameDestination.FREECELL -> listOf(
        TutorialStep(
            title = "4 Free Cells & 4 Foundations",
            description = "Every card starts face up! Use the 4 top-left Free Cells (FC) to park 1 card each while building the 4 top-right Foundations from Ace to King.",
            visualType = TutorialVisualType.FC_TOP_CELLS
        ),
        TutorialStep(
            title = "Cascades & Supermoves",
            description = "Build alternating red/black descending runs on the 8 columns. You can move an ordered stack at once when you have enough open Free Cells or empty columns.",
            visualType = TutorialVisualType.FC_SUPERMOVE
        ),
        TutorialStep(
            title = "Keep Free Cells Open",
            description = "Empty Free Cells and empty columns multiply your mobility. Safe cards can automatically fly to Foundations—toggle Auto-Move in Game Settings.",
            visualType = TutorialVisualType.FC_SPACE
        ),
        TutorialStep(
            title = "Undo, Hint & Replay",
            description = "Stuck on a tricky deal? Use Undo to rewind moves, Hint to spot a legal play, or Replay to restart the exact numbered deal.",
            visualType = TutorialVisualType.FC_CONTROLS
        )
    )

    GameDestination.SUDOKU -> listOf(
        TutorialStep(
            title = "Complete the 9×9 Grid",
            description = "Fill every empty cell with a digit from 1 to 9. Tap any cell on the board, then tap a number on the bottom keypad to place it.",
            visualType = TutorialVisualType.SUDOKU_GRID
        ),
        TutorialStep(
            title = "Row, Column & Box Rule",
            description = "Each digit 1–9 can appear only once in every row, every column, and every 3×3 outlined box. Selecting a cell highlights its peers.",
            visualType = TutorialVisualType.SUDOKU_PEERS
        ),
        TutorialStep(
            title = "Use Notes for Candidates",
            description = "Toggle the Notes button to pencil small candidate digits inside an empty cell while narrowing down the right number.",
            visualType = TutorialVisualType.SUDOKU_NOTES
        ),
        TutorialStep(
            title = "Keypad, Hints & Difficulty",
            description = "Each key shows how many of that digit remain. Use Erase, Undo, or Hint anytime, and pick Easy to Expert in Game Settings.",
            visualType = TutorialVisualType.SUDOKU_TOOLS
        )
    )
}

internal fun gameTitleFor(game: GameDestination): String = when (game) {
    GameDestination.MINESWEEPER -> "Minesweeper"
    GameDestination.SOLITAIRE -> "Solitaire"
    GameDestination.FREECELL -> "FreeCell"
    GameDestination.SUDOKU -> "Sudoku"
}

internal fun gameAccentFor(game: GameDestination): Color = when (game) {
    GameDestination.MINESWEEPER -> Color(0xFF2563EB)
    GameDestination.SOLITAIRE -> Color(0xFF16A34A)
    GameDestination.FREECELL -> Color(0xFF0D9488)
    GameDestination.SUDOKU -> Color(0xFFD97706)
}

@Composable
fun GameTutorialDialog(
    game: GameDestination,
    isOledMode: Boolean,
    onDismiss: () -> Unit
) {
    val steps = remember(game) { tutorialStepsFor(game) }
    var currentStepIndex by remember(game) { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]
    val colors = rememberArcadeThemeColors(isOledMode)
    val effectiveOled = LocalIsDarkTheme.current && isOledMode
    val accent = gameAccentFor(game)
    val isLastStep = currentStepIndex == steps.lastIndex

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${gameTitleFor(game)} Tutorial",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Step ${currentStepIndex + 1} of ${steps.size}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }

                if (!isLastStep) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tutorial_skip_button")
                    ) {
                        Text(
                            text = "Skip",
                            color = colors.secondaryText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("game_tutorial_dialog"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (effectiveOled) Color(0xFF07090D) else colors.screenBackground
                        )
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TutorialIllustration(
                        visualType = step.visualType,
                        isOledMode = effectiveOled,
                        accent = accent
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = step.title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = step.description,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Step progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { idx ->
                        val active = idx == currentStepIndex
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(if (active) 20.dp else 7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (active) accent else colors.secondaryText.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = { currentStepIndex-- },
                        modifier = Modifier.testTag("tutorial_back_button")
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (isLastStep) {
                            onDismiss()
                        } else {
                            currentStepIndex++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.testTag(if (isLastStep) "tutorial_done_button" else "tutorial_next_button")
                ) {
                    Text(
                        text = if (isLastStep) "Start Playing" else "Next",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

@Composable
private fun TutorialIllustration(
    visualType: TutorialVisualType,
    isOledMode: Boolean,
    accent: Color
) {
    val isDark = LocalIsDarkTheme.current
    when (visualType) {
        TutorialVisualType.MINE_UNCOVER -> MiniMinesGrid(
            cells = listOf(
                listOf(" ", " ", " "),
                listOf(" ", "1", "2"),
                listOf(" ", "1", " ")
            ),
            highlightCenter = true,
            isOledMode = isOledMode,
            isDark = isDark
        )

        TutorialVisualType.MINE_NUMBERS -> MiniMinesGrid(
            cells = listOf(
                listOf("🚩", " ", " "),
                listOf(" ", "1", " "),
                listOf(" ", " ", " ")
            ),
            highlightNeighbors = true,
            isOledMode = isOledMode,
            isDark = isDark
        )

        TutorialVisualType.MINE_FLAG -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isOledMode) Color(0xFF111827) else Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⛏️ DIG",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDC2626))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🚩 FLAG",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniMineTile(" ", isOledMode = isOledMode, isDark = isDark)
                Text("→", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                MiniMineTile("🚩", isOledMode = isOledMode, isDark = isDark)
            }
        }

        TutorialVisualType.MINE_CHORD -> MiniMinesGrid(
            cells = listOf(
                listOf("🚩", "✨", " "),
                listOf(" ", "2", "🚩"),
                listOf("✨", "✨", " ")
            ),
            highlightCenter = true,
            isOledMode = isOledMode,
            isDark = isDark
        )

        TutorialVisualType.SOL_FOUNDATIONS -> Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayingCardView(
                card = PlayingCard(Suit.SPADES, Rank.ACE),
                modifier = Modifier.width(42.dp)
            )
            PlayingCardView(
                card = PlayingCard(Suit.HEARTS, Rank.TWO),
                modifier = Modifier.width(42.dp)
            )
            EmptyCardSlot(
                label = "♣",
                isOledMode = isOledMode,
                modifier = Modifier.width(42.dp)
            )
            EmptyCardSlot(
                label = "♦",
                isOledMode = isOledMode,
                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                modifier = Modifier.width(42.dp)
            )
        }

        TutorialVisualType.SOL_CASCADE -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.height(92.dp)) {
                PlayingCardView(
                    card = PlayingCard(Suit.SPADES, Rank.SEVEN),
                    modifier = Modifier.width(44.dp)
                )
                PlayingCardView(
                    card = PlayingCard(Suit.HEARTS, Rank.SIX),
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .width(44.dp)
                )
                PlayingCardView(
                    card = PlayingCard(Suit.CLUBS, Rank.FIVE),
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .width(44.dp)
                )
            }
            EmptyCardSlot(
                label = "K",
                isOledMode = isOledMode,
                modifier = Modifier.width(44.dp)
            )
        }

        TutorialVisualType.SOL_STOCK -> Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayingCardView(
                card = PlayingCard(Suit.SPADES, Rank.ACE, isFaceUp = false),
                modifier = Modifier.width(44.dp)
            )
            Text("→", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.width(68.dp)) {
                PlayingCardView(
                    card = PlayingCard(Suit.DIAMONDS, Rank.FOUR),
                    modifier = Modifier.width(44.dp)
                )
                PlayingCardView(
                    card = PlayingCard(Suit.CLUBS, Rank.NINE),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .width(44.dp)
                )
                PlayingCardView(
                    card = PlayingCard(Suit.HEARTS, Rank.QUEEN),
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .width(44.dp)
                )
            }
        }

        TutorialVisualType.SOL_CONTROLS, TutorialVisualType.FC_CONTROLS -> Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (visualType == TutorialVisualType.SOL_CONTROLS) {
                ArcadePillButton(
                    label = "Auto",
                    selected = true,
                    isOledMode = isOledMode,
                    accentColor = Color(0xFF16A34A),
                    onClick = {}
                )
            }
            ArcadePillButton(label = "Undo", selected = false, isOledMode = isOledMode, onClick = {})
            ArcadePillButton(label = "Hint", selected = false, isOledMode = isOledMode, onClick = {})
            ArcadePillButton(label = "Replay", selected = false, isOledMode = isOledMode, onClick = {})
        }

        TutorialVisualType.FC_TOP_CELLS -> Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmptyCardSlot(label = "FC", isOledMode = isOledMode, modifier = Modifier.width(38.dp))
            PlayingCardView(
                card = PlayingCard(Suit.DIAMONDS, Rank.KING),
                modifier = Modifier.width(38.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            PlayingCardView(
                card = PlayingCard(Suit.HEARTS, Rank.ACE),
                modifier = Modifier.width(38.dp)
            )
            EmptyCardSlot(label = "A", isOledMode = isOledMode, modifier = Modifier.width(38.dp))
        }

        TutorialVisualType.FC_SUPERMOVE -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.height(92.dp)) {
                PlayingCardView(
                    card = PlayingCard(Suit.SPADES, Rank.NINE),
                    isSelected = true,
                    modifier = Modifier.width(44.dp)
                )
                PlayingCardView(
                    card = PlayingCard(Suit.DIAMONDS, Rank.EIGHT),
                    isSelected = true,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .width(44.dp)
                )
                PlayingCardView(
                    card = PlayingCard(Suit.CLUBS, Rank.SEVEN),
                    isSelected = true,
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .width(44.dp)
                )
            }
            Text("→", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            PlayingCardView(
                card = PlayingCard(Suit.HEARTS, Rank.TEN),
                modifier = Modifier.width(44.dp)
            )
        }

        TutorialVisualType.FC_SPACE -> Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmptyCardSlot(label = "FC", isOledMode = isOledMode, isHighlighted = true, modifier = Modifier.width(42.dp))
            EmptyCardSlot(label = "FC", isOledMode = isOledMode, isHighlighted = true, modifier = Modifier.width(42.dp))
            EmptyCardSlot(label = "FC", isOledMode = isOledMode, modifier = Modifier.width(42.dp))
            EmptyCardSlot(label = "FC", isOledMode = isOledMode, modifier = Modifier.width(42.dp))
        }

        TutorialVisualType.SUDOKU_GRID -> MiniSudokuBox(
            values = listOf(
                listOf("5", "3", ""),
                listOf("6", "", "9"),
                listOf("", "8", "1")
            ),
            selectedCoord = 1 to 1,
            highlightBox = false,
            isOledMode = isOledMode,
            isDark = isDark
        )

        TutorialVisualType.SUDOKU_PEERS -> MiniSudokuBox(
            values = listOf(
                listOf("4", "7", "2"),
                listOf("1", "5", "8"),
                listOf("9", "6", "3")
            ),
            selectedCoord = 1 to 1,
            highlightBox = true,
            isOledMode = isOledMode,
            isDark = isDark
        )

        TutorialVisualType.SUDOKU_NOTES -> Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isOledMode) Color(0xFF0F172A) else if (isDark) Color(0xFF1E293B) else Color.White)
                .border(1.5.dp, accent, RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val notes = setOf(2, 4, 7)
                for (r in 0 until 3) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        for (c in 0 until 3) {
                            val d = r * 3 + c + 1
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (d in notes) {
                                    Text(
                                        text = d.toString(),
                                        color = if (isOledMode || isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        TutorialVisualType.SUDOKU_TOOLS -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ArcadePillButton(label = "Undo", selected = false, isOledMode = isOledMode, onClick = {})
                ArcadePillButton(label = "Notes ON", selected = true, isOledMode = isOledMode, onClick = {})
                ArcadePillButton(label = "Hint", selected = false, isOledMode = isOledMode, onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("1" to "4", "2" to "2", "3" to "1", "4" to "3").forEach { (digit, rem) ->
                    Column(
                        modifier = Modifier
                            .width(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isOledMode) Color(0xFF111827) else Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                            .padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(digit, color = Color(0xFF60A5FA), fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(rem, color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMinesGrid(
    cells: List<List<String>>,
    highlightCenter: Boolean = false,
    highlightNeighbors: Boolean = false,
    isOledMode: Boolean,
    isDark: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        cells.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEachIndexed { c, value ->
                    val isCenter = r == 1 && c == 1
                    MiniMineTile(
                        label = value,
                        isHighlighted = (highlightCenter && isCenter) || (highlightNeighbors && !isCenter),
                        isOledMode = isOledMode,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMineTile(
    label: String,
    isHighlighted: Boolean = false,
    isOledMode: Boolean,
    isDark: Boolean
) {
    val isRevealed = label == "1" || label == "2"
    val bg = when {
        label == "✨" -> Color(0xFF16A34A).copy(alpha = 0.25f)
        isRevealed -> if (isOledMode) Color(0xFF0A0E17) else if (isDark) Color(0xFF1B202B) else Color(0xFFF1F5F9)
        isOledMode -> Color(0xFF1C2331)
        isDark -> Color(0xFF2D3545)
        else -> Color(0xFFD0D7DE)
    }
    val textColor = when (label) {
        "1" -> Color(0xFF4FC3F7)
        "2" -> Color(0xFF4ADE80)
        "✨" -> Color(0xFF4ADE80)
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(
                width = if (isHighlighted) 1.5.dp else 1.dp,
                color = if (isHighlighted) Color(0xFF60A5FA) else Color(0xFF334155),
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = JetBrainsMonoFontFamily
        )
    }
}

@Composable
private fun MiniSudokuBox(
    values: List<List<String>>,
    selectedCoord: Pair<Int, Int>,
    highlightBox: Boolean,
    isOledMode: Boolean,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.5.dp, Color(0xFF64748B), RoundedCornerShape(6.dp))
    ) {
        values.forEachIndexed { r, row ->
            Row {
                row.forEachIndexed { c, value ->
                    val isSelected = selectedCoord == (r to c)
                    val bg = when {
                        isSelected -> Color(0xFF1D4ED8).copy(alpha = 0.55f)
                        highlightBox -> if (isOledMode) Color(0xFF0F172A) else Color(0xFF1E293B)
                        isOledMode -> Color.Black
                        isDark -> Color(0xFF1E293B)
                        else -> Color(0xFFF8FAFC)
                    }
                    val textColor = if (r == 1 && c == 2 || r == 2 && c == 1) {
                        Color(0xFF60A5FA)
                    } else if (isOledMode || isDark || highlightBox) {
                        Color.White
                    } else {
                        Color(0xFF0F172A)
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(bg)
                            .border(0.5.dp, Color(0xFF475569)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }
    }
}
