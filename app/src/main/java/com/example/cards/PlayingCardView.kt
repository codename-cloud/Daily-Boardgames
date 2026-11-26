package com.example.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSelectedGold
import com.example.ui.theme.LocalIsDarkTheme

@Suppress("UNUSED_PARAMETER")
@Composable
fun PlayingCardView(
    card: PlayingCard,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isHinted: Boolean = false,
    isOledMode: Boolean = false,
    cornerFontSize: TextUnit = 13.sp,
    centerFontSize: TextUnit = 22.sp,
    cornerRadius: Dp = 5.dp,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(cornerRadius)

    // Keep card front & back colors identical between OLED and non-OLED modes
    val faceBg = Color.White
    val backBg = Color(0xFF1E3A8A)

    val targetBorderColor = when {
        isSelected -> CardSelectedGold
        isHinted -> Color(0xFF38BDF8)
        card.isFaceUp -> Color(0xFF334155)
        else -> Color(0xFF60A5FA).copy(alpha = 0.55f)
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 150),
        label = "card_border_color"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else if (isOledMode) 0.dp else 2.dp,
        animationSpec = tween(durationMillis = 150),
        label = "card_shadow_elevation"
    )
    val borderWidth = if (isSelected || isHinted) 2.dp else 1.dp

    val cardDescription = if (card.isFaceUp) {
        "${card.rank.label} of ${card.suit.displayName}"
    } else {
        "Face-down card"
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(0.70f)
            .shadow(
                elevation = shadowElevation,
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
        val cardWidth = maxWidth
        val cardHeight = maxHeight
        val density = LocalDensity.current

        if (card.isFaceUp) {
            val inkColor = if (card.isRed) Color(0xFFEB1414) else Color(0xFF111111)
            val isCourt = card.rank.value >= 11
            val isTen = card.rank == Rank.TEN

            val headerHeight = cardHeight * 0.28f
            val hPad = (cardWidth * 0.055f).coerceAtLeast(2.dp)
            val rankFontDp = if (isTen) cardWidth * 0.295f else cardWidth * 0.35f
            val rankFontSize = with(density) { rankFontDp.toSp() }
            val topSuitSize = (cardWidth * 0.27f).coerceAtLeast(9.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                // Large-index top bar: bold rank on top-left, crisp suit pip on top-right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .padding(horizontal = hPad, vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.rank.label,
                        color = inkColor,
                        style = TextStyle(
                            fontSize = rankFontSize,
                            lineHeight = rankFontSize,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = if (isTen) (-0.9).sp else (-0.2).sp,
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

                    SuitSymbolCanvas(
                        suit = card.suit,
                        color = inkColor,
                        modifier = Modifier.size(topSuitSize)
                    )
                }

                // Lower card body: framed royal illustration for J/Q/K, giant crisp suit pip for A-10
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(
                            start = if (isCourt) hPad else 2.dp,
                            end = if (isCourt) hPad else 2.dp,
                            bottom = if (isCourt) hPad else 3.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCourt) {
                        CourtCardArtwork(
                            rank = card.rank,
                            suit = card.suit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val centerPipSize = minOf(cardWidth * 0.72f, cardHeight * 0.50f)
                        SuitSymbolCanvas(
                            suit = card.suit,
                            color = inkColor,
                            modifier = Modifier.size(centerPipSize)
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CardSelectedGold.copy(alpha = 0.16f))
                )
            } else if (isHinted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF38BDF8).copy(alpha = 0.16f))
                )
            }
        } else {
            // Crisp slate-blue geometric card back matching Minesweeper tiles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.5.dp)
                    .clip(RoundedCornerShape((cornerRadius - 2.dp).coerceAtLeast(2.dp)))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2563EB), Color(0xFF1E3A8A))
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape((cornerRadius - 2.dp).coerceAtLeast(2.dp))
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 9.dp.toPx()
                    val lineColor = Color(0xFF93C5FD).copy(alpha = 0.35f)
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
    cornerRadius: Dp = 5.dp,
    onClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val effectiveOled = isDark && isOledMode
    val shape = RoundedCornerShape(cornerRadius)
    val slotBg = when {
        effectiveOled -> Color.Black
        isDark -> Color(0xFF1E293B).copy(alpha = 0.7f)
        else -> Color(0xFFCBD5E1).copy(alpha = 0.65f)
    }
    val slotBorder = when {
        isHighlighted -> CardSelectedGold
        effectiveOled -> Color(0xFF334155)
        isDark -> Color(0xFF334155)
        else -> Color(0xFF94A3B8)
    }
    val labelTint = tint ?: when {
        effectiveOled -> Color(0xFF64748B)
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
