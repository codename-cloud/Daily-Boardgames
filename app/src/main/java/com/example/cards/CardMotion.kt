package com.example.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BoardCardPlacement(
    val card: PlayingCard,
    val pileKey: String,
    val targetOffsetPx: Offset,
    val entranceOriginPx: Offset? = null,
    val entranceDelayMs: Int = 0,
    val entranceFlip: Boolean = false,
    val restZIndex: Float,
    val isSelected: Boolean = false,
    val isHinted: Boolean = false,
    val onClick: (() -> Unit)? = null
)

@Composable
fun AnimatedBoardCard(
    placement: BoardCardPlacement,
    cardWidth: Dp,
    cardHeight: Dp,
    dealGeneration: Int,
    boardSizeKey: Long,
    isOledMode: Boolean,
    cornerFont: TextUnit,
    centerFont: TextUnit
) {
    val density = LocalDensity.current

    val initialPos = placement.entranceOriginPx ?: placement.targetOffsetPx
    val initialFlip = when {
        placement.entranceFlip -> 180f
        placement.card.isFaceUp -> 0f
        else -> 180f
    }

    val posAnim = remember { Animatable(initialPos, Offset.VectorConverter) }
    val flipAnim = remember { Animatable(initialFlip) }

    var lastPileKey by remember { mutableStateOf(placement.pileKey) }
    var lastDealGen by remember { mutableIntStateOf(dealGeneration) }
    var lastBoardSizeKey by remember { mutableLongStateOf(boardSizeKey) }
    var lastFaceUp by remember {
        mutableStateOf(if (placement.entranceFlip) false else placement.card.isFaceUp)
    }
    var hasAnimatedEntrance by remember { mutableStateOf(false) }
    var isFlightActive by remember { mutableStateOf(placement.entranceOriginPx != null) }

    val pileChangedSynchronously =
        dealGeneration == lastDealGen &&
            boardSizeKey == lastBoardSizeKey &&
            placement.pileKey != lastPileKey
    val inFlight = isFlightActive || pileChangedSynchronously

    LaunchedEffect(
        placement.targetOffsetPx,
        placement.pileKey,
        placement.card.isFaceUp,
        dealGeneration,
        boardSizeKey
    ) {
        if (dealGeneration != lastDealGen || boardSizeKey != lastBoardSizeKey) {
            lastDealGen = dealGeneration
            lastBoardSizeKey = boardSizeKey
            lastPileKey = placement.pileKey
            lastFaceUp = placement.card.isFaceUp
            hasAnimatedEntrance = true
            isFlightActive = false
            posAnim.snapTo(placement.targetOffsetPx)
            flipAnim.snapTo(if (placement.card.isFaceUp) 0f else 180f)
            return@LaunchedEffect
        }

        val pileChanged = placement.pileKey != lastPileKey
        val faceChanged = placement.card.isFaceUp != lastFaceUp
        val isEntrance = !hasAnimatedEntrance && (placement.entranceOriginPx != null || placement.entranceFlip)
        hasAnimatedEntrance = true

        lastPileKey = placement.pileKey
        lastFaceUp = placement.card.isFaceUp

        if (pileChanged || isEntrance) {
            isFlightActive = true
        }

        if (isEntrance && placement.entranceDelayMs > 0) {
            delay(placement.entranceDelayMs.toLong())
        }

        val moveJob = if (posAnim.value != placement.targetOffsetPx) {
            launch {
                val spec = if (pileChanged || isEntrance) {
                    spring<Offset>(dampingRatio = 0.84f, stiffness = 560f)
                } else {
                    spring<Offset>(dampingRatio = 0.90f, stiffness = 640f)
                }
                posAnim.animateTo(
                    targetValue = placement.targetOffsetPx,
                    animationSpec = spec
                )
            }
        } else null

        val flipJob = if (faceChanged || (isEntrance && placement.entranceFlip)) {
            launch {
                flipAnim.animateTo(
                    targetValue = if (placement.card.isFaceUp) 0f else 180f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
        } else null

        moveJob?.join()
        flipJob?.join()
        isFlightActive = false
    }

    val scale by animateFloatAsState(
        targetValue = when {
            inFlight -> 1.045f
            placement.isSelected -> 1.035f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 580f),
        label = "card_scale"
    )

    val selectionLiftPx by animateFloatAsState(
        targetValue = if (placement.isSelected) with(density) { (-4).dp.toPx() } else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 600f),
        label = "card_selection_lift"
    )

    val flipDeg = flipAnim.value
    val showFace = flipDeg <= 90f
    val renderedCard = if (showFace == placement.card.isFaceUp) {
        placement.card
    } else {
        placement.card.copy(isFaceUp = showFace)
    }
    val rotationYDeg = if (flipDeg > 90f) 180f - flipDeg else -flipDeg

    val effectiveZIndex = when {
        inFlight -> 100f + placement.restZIndex
        placement.isSelected -> placement.restZIndex + 0.5f
        else -> placement.restZIndex
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = posAnim.value.x.roundToInt(),
                    y = (posAnim.value.y + selectionLiftPx).roundToInt()
                )
            }
            .zIndex(effectiveZIndex)
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationY = rotationYDeg
                cameraDistance = 12f * density.density
            }
    ) {
        PlayingCardView(
            card = renderedCard,
            isOledMode = isOledMode,
            isSelected = placement.isSelected,
            isHinted = placement.isHinted,
            cornerFontSize = cornerFont,
            centerFontSize = centerFont,
            onClick = placement.onClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}
