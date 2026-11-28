package com.example.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ConfettiShape {
    RIBBON,
    SPARKLE,
    DOT
}

private data class ConfettiParticle(
    val startXFrac: Float,
    val startYFrac: Float,
    val vxFrac: Float,
    val vyFrac: Float,
    val gravityFrac: Float,
    val swayAmpPx: Float,
    val swayFreq: Float,
    val spinSpeed: Float,
    val initialAngle: Float,
    val baseSizeDp: Float,
    val delayFrac: Float,
    val color: Color,
    val shape: ConfettiShape
)

private val CelebrationColors = listOf(
    Color(0xFF22C55E), // Emerald
    Color(0xFFFACC15), // Gold
    Color(0xFF38BDF8), // Sky
    Color(0xFFFB923C), // Orange
    Color(0xFFF43F5E), // Coral
    Color(0xFFA855F7)  // Violet
)

@Composable
fun DailySweepCelebrationOverlay(
    visible: Boolean,
    completedCount: Int,
    totalCount: Int,
    isOledMode: Boolean,
    isDark: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val progress = remember { Animatable(0f) }
    val bannerScale = remember { Animatable(0.76f) }
    val bannerAlpha = remember { Animatable(0f) }

    val particles = remember {
        val rng = Random(20261127)
        List(78) { index ->
            val fromLeft = index % 3 == 1
            val fromRight = index % 3 == 2
            val startX = when {
                fromLeft -> 0.14f + rng.nextFloat() * 0.12f
                fromRight -> 0.74f + rng.nextFloat() * 0.12f
                else -> 0.35f + rng.nextFloat() * 0.30f
            }
            val startY = when {
                fromLeft || fromRight -> 0.24f + rng.nextFloat() * 0.12f
                else -> 0.18f + rng.nextFloat() * 0.10f
            }
            val vx = when {
                fromLeft -> 0.10f + rng.nextFloat() * 0.45f
                fromRight -> -(0.10f + rng.nextFloat() * 0.45f)
                else -> (rng.nextFloat() - 0.5f) * 0.85f
            }
            val vy = -(0.28f + rng.nextFloat() * 0.46f)
            ConfettiParticle(
                startXFrac = startX,
                startYFrac = startY,
                vxFrac = vx,
                vyFrac = vy,
                gravityFrac = 0.95f + rng.nextFloat() * 0.55f,
                swayAmpPx = 8f + rng.nextFloat() * 18f,
                swayFreq = 4f + rng.nextFloat() * 6f,
                spinSpeed = (rng.nextFloat() - 0.5f) * 720f,
                initialAngle = rng.nextFloat() * 360f,
                baseSizeDp = 6f + rng.nextFloat() * 6f,
                delayFrac = if (index < 48) 0f else 0.08f + rng.nextFloat() * 0.08f,
                color = CelebrationColors[index % CelebrationColors.size],
                shape = when (index % 5) {
                    0 -> ConfettiShape.SPARKLE
                    1 -> ConfettiShape.DOT
                    else -> ConfettiShape.RIBBON
                }
            )
        }
    }

    LaunchedEffect(visible) {
        progress.snapTo(0f)
        bannerScale.snapTo(0.76f)
        bannerAlpha.snapTo(0f)

        val confettiJob = launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
            )
        }
        val bannerInJob = launch {
            launch {
                bannerAlpha.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
            launch {
                bannerScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 360f)
                )
            }
        }

        bannerInJob.join()
        delay(2350)
        bannerAlpha.animateTo(0f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        confettiJob.join()
        onFinished()
    }

    val cardBg = when {
        isOledMode -> Color(0xFF0A0A0A)
        isDark -> Color(0xFF1E293B)
        else -> Color.White
    }
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val badgeBg = Color(0xFF16A34A).copy(alpha = if (isDark) 0.22f else 0.14f)
    val badgeText = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rawT = progress.value
            val w = size.width
            val h = size.height
            val diamondPath = Path()

            for (p in particles) {
                val localT = ((rawT - p.delayFrac) / (1f - p.delayFrac)).coerceIn(0f, 1f)
                if (localT <= 0f) continue

                val fadeOut = if (localT > 0.72f) {
                    (1f - (localT - 0.72f) / 0.28f).coerceIn(0f, 1f)
                } else {
                    1f
                }
                if (fadeOut <= 0.01f) continue

                val sway = sin(localT * p.swayFreq * PI.toFloat()) * p.swayAmpPx * density
                val px = p.startXFrac * w + p.vxFrac * w * localT + sway
                val py = p.startYFrac * h + (p.vyFrac * localT + 0.5f * p.gravityFrac * localT * localT) * h
                val angle = p.initialAngle + p.spinSpeed * localT
                val tumbleScale = (0.35f + 0.65f * kotlin.math.abs(cos(angle * PI.toFloat() / 180f)))
                val sizePx = p.baseSizeDp * density
                val tint = p.color.copy(alpha = fadeOut * 0.95f)

                rotate(degrees = angle, pivot = Offset(px, py)) {
                    when (p.shape) {
                        ConfettiShape.RIBBON -> {
                            val rw = sizePx * 1.45f
                            val rh = sizePx * 0.62f * tumbleScale
                            drawRect(
                                color = tint,
                                topLeft = Offset(px - rw / 2f, py - rh / 2f),
                                size = Size(rw, rh)
                            )
                        }
                        ConfettiShape.SPARKLE -> {
                            val r = sizePx * 0.78f
                            diamondPath.reset()
                            diamondPath.moveTo(px, py - r)
                            diamondPath.lineTo(px + r * 0.7f, py)
                            diamondPath.lineTo(px, py + r)
                            diamondPath.lineTo(px - r * 0.7f, py)
                            diamondPath.close()
                            drawPath(path = diamondPath, color = tint)
                        }
                        ConfettiShape.DOT -> {
                            drawCircle(
                                color = tint,
                                radius = sizePx * 0.42f,
                                center = Offset(px, py)
                            )
                        }
                    }
                }
            }
        }

        // Celebratory banner centered near the upper-middle; tapping it dismisses early
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 92.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val shape = RoundedCornerShape(18.dp)
            val borderBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF22C55E), Color(0xFFFACC15), Color(0xFF38BDF8))
            )

            Row(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = bannerScale.value
                        scaleY = bannerScale.value
                        alpha = bannerAlpha.value
                    }
                    .shadow(elevation = if (isOledMode) 0.dp else 12.dp, shape = shape)
                    .clip(shape)
                    .background(cardBg)
                    .border(2.dp, borderBrush, shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFinished
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("home_daily_sweep_celebration"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFACC15).copy(alpha = 0.28f),
                                    Color(0xFF22C55E).copy(alpha = 0.16f)
                                )
                            )
                        )
                        .border(1.5.dp, Color(0xFFFACC15).copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏆", fontSize = 24.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daily Sweep!",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = titleColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$completedCount / $totalCount",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = badgeText
                            )
                        }
                    }

                    Text(
                        text = if (totalCount == 1) {
                            "Daily board conquered today!"
                        } else {
                            "All $totalCount daily boards conquered today!"
                        },
                        fontSize = 13.sp,
                        color = subtitleColor
                    )
                }
            }
        }
    }
}
