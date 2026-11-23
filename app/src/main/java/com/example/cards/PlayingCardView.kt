package com.example.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBlackSuit
import com.example.ui.theme.CardFaceIvory
import com.example.ui.theme.CardRedSuit
import com.example.ui.theme.CardSelectedGold
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun PlayingCardView(
    card: PlayingCard,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isHinted: Boolean = false,
    isOledMode: Boolean = false,
    cornerFontSize: TextUnit = 13.sp,
    centerFontSize: TextUnit = 22.sp,
    cornerRadius: Dp = 6.dp,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)

    val faceBg = when {
        isOledMode -> Color(0xFF0A0A0A)
        isDark -> Color(0xFFF8FAFC)
        else -> CardFaceIvory
    }
    val backBg = when {
        isOledMode -> Color(0xFF111827)
        else -> Color(0xFF1E3A8A)
    }

    val borderColor = when {
        isSelected -> CardSelectedGold
        isHinted -> Color(0xFF38BDF8)
        card.isFaceUp && isOledMode -> Color(0xFF334155)
        card.isFaceUp -> Color(0xFF94A3B8)
        isOledMode -> Color(0xFF334155)
        else -> Color(0xFF60A5FA).copy(alpha = 0.5f)
    }
    val borderWidth = if (isSelected || isHinted) 2.dp else 1.dp

    val cardDescription = if (card.isFaceUp) {
        "${card.rank.label} of ${card.suit.displayName}"
    } else {
        "Face-down card"
    }

    Box(
        modifier = modifier
            .aspectRatio(0.70f)
            .shadow(
                elevation = if (isSelected) 8.dp else if (isOledMode) 0.dp else 2.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(if (card.isFaceUp) faceBg else backBg)
            .border(borderWidth, borderColor, shape)
            .semantics { contentDescription = cardDescription }
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        if (card.isFaceUp) {
            val inkColor = if (isOledMode) {
                if (card.isRed) Color(0xFFFF4D6A) else Color(0xFFF8FAFC)
            } else {
                if (card.isRed) CardRedSuit else CardBlackSuit
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CardSelectedGold.copy(alpha = if (isOledMode) 0.22f else 0.14f))
                )
            } else if (isHinted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF38BDF8).copy(alpha = if (isOledMode) 0.22f else 0.14f))
                )
            }

            // Compact top strip so overlapping cascades always stay readable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.rank.label,
                    color = inkColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = cornerFontSize,
                    lineHeight = cornerFontSize,
                    maxLines = 1
                )
                Text(
                    text = card.suit.symbol,
                    color = inkColor,
                    fontSize = cornerFontSize,
                    lineHeight = cornerFontSize,
                    maxLines = 1
                )
            }

            // Center emblem
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (card.rank.value >= 11) "${card.rank.label}${card.suit.symbol}" else card.suit.symbol,
                    color = inkColor.copy(alpha = 0.92f),
                    fontSize = if (card.rank.value >= 11) centerFontSize * 0.78f else centerFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Crisp slate-blue geometric card back matching Minesweeper tiles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.5.dp)
                    .clip(RoundedCornerShape(cornerRadius - 2.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isOledMode) {
                                listOf(Color(0xFF0F172A), Color(0xFF020617))
                            } else {
                                listOf(Color(0xFF2563EB), Color(0xFF1E3A8A))
                            }
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (isOledMode) Color(0xFF334155) else Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(cornerRadius - 2.dp)
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 9.dp.toPx()
                    val lineColor = if (isOledMode) Color(0xFF334155) else Color(0xFF93C5FD).copy(alpha = 0.35f)
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = lineColor,
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = 0.9.dp.toPx()
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(x + size.height, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 0.9.dp.toPx()
                        )
                        x += step
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCardSlot(
    label: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    isOledMode: Boolean = false,
    tint: Color? = null,
    cornerRadius: Dp = 6.dp,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)
    val slotBg = when {
        isOledMode -> Color.Black
        isDark -> Color(0xFF1E293B).copy(alpha = 0.7f)
        else -> Color(0xFFCBD5E1).copy(alpha = 0.65f)
    }
    val slotBorder = when {
        isHighlighted -> CardSelectedGold
        isOledMode -> Color(0xFF334155)
        isDark -> Color(0xFF334155)
        else -> Color(0xFF94A3B8)
    }
    val labelTint = tint ?: when {
        isOledMode -> Color(0xFF64748B)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }

    Box(
        modifier = modifier
            .aspectRatio(0.70f)
            .clip(shape)
            .background(slotBg)
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = slotBorder,
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = labelTint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
