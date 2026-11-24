package com.example.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

/**
 * Draws crisp vector playing-card suit symbols (Spade, Heart, Club, Diamond)
 * matching large-index classic Solitaire / FreeCell deck proportions.
 */
internal fun DrawScope.drawSuitSymbol(
    suit: Suit,
    color: Color,
    topLeft: Offset = Offset.Zero,
    symbolSize: Size = size
) {
    val ox = topLeft.x
    val oy = topLeft.y
    val w = symbolSize.width
    val h = symbolSize.height
    val s = minOf(w, h)
    val dx = ox + (w - s) / 2f
    val dy = oy + (h - s) / 2f

    when (suit) {
        Suit.HEARTS -> {
            val path = Path().apply {
                moveTo(dx + 0.50f * s, dy + 0.25f * s)
                cubicTo(
                    dx + 0.44f * s, dy + 0.05f * s,
                    dx + 0.05f * s, dy + 0.06f * s,
                    dx + 0.05f * s, dy + 0.36f * s
                )
                cubicTo(
                    dx + 0.05f * s, dy + 0.60f * s,
                    dx + 0.30f * s, dy + 0.77f * s,
                    dx + 0.50f * s, dy + 0.94f * s
                )
                cubicTo(
                    dx + 0.70f * s, dy + 0.77f * s,
                    dx + 0.95f * s, dy + 0.60f * s,
                    dx + 0.95f * s, dy + 0.36f * s
                )
                cubicTo(
                    dx + 0.95f * s, dy + 0.06f * s,
                    dx + 0.56f * s, dy + 0.05f * s,
                    dx + 0.50f * s, dy + 0.25f * s
                )
                close()
            }
            drawPath(path = path, color = color, style = Fill)
        }

        Suit.DIAMONDS -> {
            val path = Path().apply {
                moveTo(dx + 0.50f * s, dy + 0.04f * s)
                lineTo(dx + 0.89f * s, dy + 0.50f * s)
                lineTo(dx + 0.50f * s, dy + 0.96f * s)
                lineTo(dx + 0.11f * s, dy + 0.50f * s)
                close()
            }
            drawPath(path = path, color = color, style = Fill)
        }

        Suit.SPADES -> {
            val path = Path().apply {
                moveTo(dx + 0.50f * s, dy + 0.05f * s)
                cubicTo(
                    dx + 0.32f * s, dy + 0.24f * s,
                    dx + 0.07f * s, dy + 0.43f * s,
                    dx + 0.07f * s, dy + 0.63f * s
                )
                cubicTo(
                    dx + 0.07f * s, dy + 0.78f * s,
                    dx + 0.22f * s, dy + 0.85f * s,
                    dx + 0.37f * s, dy + 0.79f * s
                )
                cubicTo(
                    dx + 0.43f * s, dy + 0.76f * s,
                    dx + 0.46f * s, dy + 0.72f * s,
                    dx + 0.47f * s, dy + 0.69f * s
                )
                cubicTo(
                    dx + 0.46f * s, dy + 0.81f * s,
                    dx + 0.41f * s, dy + 0.89f * s,
                    dx + 0.34f * s, dy + 0.95f * s
                )
                lineTo(dx + 0.66f * s, dy + 0.95f * s)
                cubicTo(
                    dx + 0.59f * s, dy + 0.89f * s,
                    dx + 0.54f * s, dy + 0.81f * s,
                    dx + 0.53f * s, dy + 0.69f * s
                )
                cubicTo(
                    dx + 0.54f * s, dy + 0.72f * s,
                    dx + 0.57f * s, dy + 0.76f * s,
                    dx + 0.63f * s, dy + 0.79f * s
                )
                cubicTo(
                    dx + 0.78f * s, dy + 0.85f * s,
                    dx + 0.93f * s, dy + 0.78f * s,
                    dx + 0.93f * s, dy + 0.63f * s
                )
                cubicTo(
                    dx + 0.93f * s, dy + 0.43f * s,
                    dx + 0.68f * s, dy + 0.24f * s,
                    dx + 0.50f * s, dy + 0.05f * s
                )
                close()
            }
            drawPath(path = path, color = color, style = Fill)
        }

        Suit.CLUBS -> {
            val r = 0.215f * s
            drawCircle(color = color, radius = r, center = Offset(dx + 0.50f * s, dy + 0.27f * s))
            drawCircle(color = color, radius = r, center = Offset(dx + 0.265f * s, dy + 0.56f * s))
            drawCircle(color = color, radius = r, center = Offset(dx + 0.735f * s, dy + 0.56f * s))
            drawCircle(color = color, radius = 0.14f * s, center = Offset(dx + 0.50f * s, dy + 0.51f * s))

            val stemPath = Path().apply {
                moveTo(dx + 0.46f * s, dy + 0.54f * s)
                lineTo(dx + 0.54f * s, dy + 0.54f * s)
                cubicTo(
                    dx + 0.54f * s, dy + 0.75f * s,
                    dx + 0.58f * s, dy + 0.86f * s,
                    dx + 0.65f * s, dy + 0.95f * s
                )
                lineTo(dx + 0.35f * s, dy + 0.95f * s)
                cubicTo(
                    dx + 0.42f * s, dy + 0.86f * s,
                    dx + 0.46f * s, dy + 0.75f * s,
                    dx + 0.46f * s, dy + 0.54f * s
                )
                close()
            }
            drawPath(path = stemPath, color = color, style = Fill)
        }
    }
}

@Composable
internal fun SuitSymbolCanvas(
    suit: Suit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawSuitSymbol(suit = suit, color = color)
    }
}

/**
 * Framed traditional red/blue/gold royal court illustration for Jack, Queen, and King.
 */
@Composable
internal fun CourtCardArtwork(
    rank: Rank,
    suit: Suit,
    modifier: Modifier = Modifier
) {
    val royalRed = Color(0xFFDF1B1B)
    val royalBlue = Color(0xFF1E56D9)
    val royalGold = Color(0xFFFACC15)
    val skinCream = Color(0xFFFFF8EC)
    val inkBlack = Color(0xFF111111)
    val frameBorder = Color(0xFF1F2937)

    val primaryRobe = if (suit.isRed) royalRed else royalBlue
    val secondaryRobe = if (suit.isRed) royalBlue else royalRed

    Box(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, frameBorder)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeW = (w * 0.025f).coerceAtLeast(1f)
            val outline = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)

            clipRect {
                // Subtle diagonal court background fill
                drawRect(color = Color.White)

                // Robe / torso base filling lower half
                val torsoPath = Path().apply {
                    moveTo(0.06f * w, 1.02f * h)
                    lineTo(0.14f * w, 0.64f * h)
                    quadraticTo(0.50f * w, 0.54f * h, 0.86f * w, 0.64f * h)
                    lineTo(0.94f * w, 1.02f * h)
                    close()
                }
                drawPath(torsoPath, color = primaryRobe)

                // Diagonal / vertical contrasting royal panel
                val panelPath = Path().apply {
                    moveTo(0.28f * w, 1.02f * h)
                    lineTo(0.36f * w, 0.60f * h)
                    lineTo(0.64f * w, 0.60f * h)
                    lineTo(0.72f * w, 1.02f * h)
                    close()
                }
                drawPath(panelPath, color = secondaryRobe)

                // Gold tunic trim & royal sash
                drawRect(
                    color = royalGold,
                    topLeft = Offset(0.45f * w, 0.61f * h),
                    size = Size(0.10f * w, 0.42f * h)
                )
                val sashPath = Path().apply {
                    moveTo(0.16f * w, 0.69f * h)
                    lineTo(0.84f * w, 0.86f * h)
                    lineTo(0.81f * w, 0.96f * h)
                    lineTo(0.13f * w, 0.79f * h)
                    close()
                }
                drawPath(sashPath, color = royalGold)

                // Outlines for torso and collar
                drawPath(torsoPath, color = inkBlack, style = outline)
                drawPath(panelPath, color = inkBlack, style = outline)
                drawPath(sashPath, color = inkBlack, style = outline)

                // White/gold ruffled collar
                val collarPath = Path().apply {
                    moveTo(0.30f * w, 0.60f * h)
                    quadraticTo(0.50f * w, 0.70f * h, 0.70f * w, 0.60f * h)
                    lineTo(0.62f * w, 0.53f * h)
                    lineTo(0.38f * w, 0.53f * h)
                    close()
                }
                drawPath(collarPath, color = royalGold)
                drawPath(collarPath, color = inkBlack, style = outline)

                // Hair / Hood framing face
                val hairColor = when (rank) {
                    Rank.KING -> Color(0xFFFDE047)
                    Rank.QUEEN -> Color(0xFF1E3A8A)
                    else -> Color(0xFFFACC15)
                }
                drawOval(
                    color = hairColor,
                    topLeft = Offset(0.28f * w, 0.25f * h),
                    size = Size(0.44f * w, 0.34f * h)
                )
                drawOval(
                    color = inkBlack,
                    topLeft = Offset(0.28f * w, 0.25f * h),
                    size = Size(0.44f * w, 0.34f * h),
                    style = outline
                )

                // Face oval
                val faceRect = Rect(
                    left = 0.34f * w,
                    top = 0.28f * h,
                    right = 0.66f * w,
                    bottom = 0.56f * h
                )
                drawOval(
                    color = skinCream,
                    topLeft = faceRect.topLeft,
                    size = faceRect.size
                )
                drawOval(
                    color = inkBlack,
                    topLeft = faceRect.topLeft,
                    size = faceRect.size,
                    style = outline
                )

                // Crisp facial features
                val eyeY = 0.39f * h
                val eyeR = (w * 0.022f).coerceAtLeast(1.2f)
                drawCircle(color = inkBlack, radius = eyeR, center = Offset(0.43f * w, eyeY))
                drawCircle(color = inkBlack, radius = eyeR, center = Offset(0.57f * w, eyeY))
                // Nose line
                drawLine(
                    color = inkBlack,
                    start = Offset(0.50f * w, 0.39f * h),
                    end = Offset(0.49f * w, 0.46f * h),
                    strokeWidth = strokeW * 0.8f,
                    cap = StrokeCap.Round
                )
                // Mouth / beard
                if (rank == Rank.KING) {
                    val beardPath = Path().apply {
                        moveTo(0.36f * w, 0.48f * h)
                        quadraticTo(0.50f * w, 0.63f * h, 0.64f * w, 0.48f * h)
                        quadraticTo(0.50f * w, 0.53f * h, 0.36f * w, 0.48f * h)
                        close()
                    }
                    drawPath(beardPath, color = royalGold)
                    drawPath(beardPath, color = inkBlack, style = outline)
                } else {
                    drawLine(
                        color = royalRed,
                        start = Offset(0.45f * w, 0.49f * h),
                        end = Offset(0.55f * w, 0.49f * h),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Rank-specific Crown / Cap + Scepter / Sword
                when (rank) {
                    Rank.KING -> {
                        val crown = Path().apply {
                            moveTo(0.31f * w, 0.30f * h)
                            lineTo(0.28f * w, 0.12f * h)
                            lineTo(0.40f * w, 0.21f * h)
                            lineTo(0.50f * w, 0.08f * h)
                            lineTo(0.60f * w, 0.21f * h)
                            lineTo(0.72f * w, 0.12f * h)
                            lineTo(0.69f * w, 0.30f * h)
                            close()
                        }
                        drawPath(crown, color = royalGold)
                        drawPath(crown, color = inkBlack, style = outline)
                        // Jewels on crown band
                        drawCircle(color = royalRed, radius = eyeR * 1.3f, center = Offset(0.50f * w, 0.25f * h))
                        drawCircle(color = royalBlue, radius = eyeR * 1.1f, center = Offset(0.38f * w, 0.26f * h))
                        drawCircle(color = royalBlue, radius = eyeR * 1.1f, center = Offset(0.62f * w, 0.26f * h))

                        // Raised royal sword on right
                        drawLine(
                            color = Color(0xFF94A3B8),
                            start = Offset(0.86f * w, 0.16f * h),
                            end = Offset(0.86f * w, 0.66f * h),
                            strokeWidth = strokeW * 1.8f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = inkBlack,
                            start = Offset(0.86f * w, 0.16f * h),
                            end = Offset(0.86f * w, 0.66f * h),
                            strokeWidth = strokeW * 0.7f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = royalGold,
                            start = Offset(0.78f * w, 0.56f * h),
                            end = Offset(0.94f * w, 0.56f * h),
                            strokeWidth = strokeW * 1.6f,
                            cap = StrokeCap.Round
                        )
                    }

                    Rank.QUEEN -> {
                        val tiara = Path().apply {
                            moveTo(0.32f * w, 0.29f * h)
                            lineTo(0.35f * w, 0.15f * h)
                            lineTo(0.43f * w, 0.22f * h)
                            lineTo(0.50f * w, 0.10f * h)
                            lineTo(0.57f * w, 0.22f * h)
                            lineTo(0.65f * w, 0.15f * h)
                            lineTo(0.68f * w, 0.29f * h)
                            close()
                        }
                        drawPath(tiara, color = royalGold)
                        drawPath(tiara, color = inkBlack, style = outline)
                        drawCircle(color = royalRed, radius = eyeR * 1.4f, center = Offset(0.50f * w, 0.23f * h))

                        // Classic rosette flower held on left (like Q♦ / Q♠ in screenshot)
                        drawLine(
                            color = inkBlack,
                            start = Offset(0.15f * w, 0.62f * h),
                            end = Offset(0.22f * w, 0.84f * h),
                            strokeWidth = strokeW * 1.1f
                        )
                        drawCircle(color = royalGold, radius = w * 0.08f, center = Offset(0.14f * w, 0.58f * h))
                        drawCircle(color = inkBlack, radius = w * 0.08f, center = Offset(0.14f * w, 0.58f * h), style = outline)
                        drawCircle(color = royalRed, radius = w * 0.032f, center = Offset(0.14f * w, 0.58f * h))
                    }

                    Rank.JACK -> {
                        // Flat red & gold Tudor cap (like J♣ in screenshot)
                        val cap = Path().apply {
                            moveTo(0.24f * w, 0.26f * h)
                            lineTo(0.26f * w, 0.14f * h)
                            lineTo(0.74f * w, 0.14f * h)
                            lineTo(0.76f * w, 0.26f * h)
                            close()
                        }
                        drawPath(cap, color = royalRed)
                        drawPath(cap, color = inkBlack, style = outline)
                        drawRoundRect(
                            color = royalGold,
                            topLeft = Offset(0.29f * w, 0.24f * h),
                            size = Size(0.42f * w, 0.06f * h),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        drawRoundRect(
                            color = inkBlack,
                            topLeft = Offset(0.29f * w, 0.24f * h),
                            size = Size(0.42f * w, 0.06f * h),
                            cornerRadius = CornerRadius(2f, 2f),
                            style = outline
                        )
                    }

                    else -> Unit
                }

                // Small suit pip badge inside court card frame
                val pipSize = minOf(w, h) * 0.22f
                drawSuitSymbol(
                    suit = suit,
                    color = if (suit.isRed) royalRed else inkBlack,
                    topLeft = Offset(w - pipSize - 0.06f * w, 0.05f * h),
                    symbolSize = Size(pipSize, pipSize)
                )
            }
        }
    }
}
