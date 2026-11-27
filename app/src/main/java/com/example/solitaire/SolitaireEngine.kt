package com.example.solitaire

import com.example.cards.PlayingCard
import com.example.cards.Rank
import com.example.cards.createStandardDeck
import kotlin.random.Random

object SolitaireEngine {

    private fun dealFromSeed(drawMode: SolitaireDrawMode, seed: Long?): SolitaireGameState {
        val deck = createStandardDeck(seed = seed, faceUp = false)
        val tableau = MutableList(7) { mutableListOf<PlayingCard>() }
        var cursor = 0

        for (col in 0 until 7) {
            for (row in 0..col) {
                val isTop = row == col
                tableau[col].add(deck[cursor++].copy(isFaceUp = isTop))
            }
        }

        val stock = deck.subList(cursor, deck.size).map { it.copy(isFaceUp = false) }
        return SolitaireGameState(
            stock = stock,
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = tableau.map { it.toList() },
            drawMode = drawMode,
            moves = 0,
            score = 0
        )
    }

    fun newDeal(drawMode: SolitaireDrawMode = SolitaireDrawMode.DRAW_1, seed: Long? = null): SolitaireGameState {
        val baseSeed = seed ?: Random.nextLong()
        val maxAttempts = if (drawMode == SolitaireDrawMode.DRAW_3) 120 else 60
        for (attempt in 0 until maxAttempts) {
            val candidateSeed = baseSeed + attempt * 7919L
            val deal = dealFromSeed(drawMode, candidateSeed)
            if (SolitaireSolver.isWinnable(deal)) {
                return deal
            }
        }
        for (fallbackSeed in 1L..500L) {
            val deal = dealFromSeed(drawMode, fallbackSeed)
            if (SolitaireSolver.isWinnable(deal)) {
                return deal
            }
        }
        return dealFromSeed(drawMode, baseSeed)
    }

    fun drawFromStock(state: SolitaireGameState): SolitaireGameState? {
        if (state.stock.isEmpty() && state.waste.isEmpty()) return null

        return if (state.stock.isNotEmpty()) {
            val count = minOf(state.drawMode.drawCount, state.stock.size)
            val drawn = state.stock.takeLast(count).reversed().map { it.copy(isFaceUp = true) }
            val remainingStock = state.stock.dropLast(count)
            state.copy(
                stock = remainingStock,
                waste = state.waste + drawn,
                moves = state.moves + 1
            )
        } else {
            // Recycle waste back to stock
            val recycledStock = state.waste.reversed().map { it.copy(isFaceUp = false) }
            state.copy(
                stock = recycledStock,
                waste = emptyList(),
                moves = state.moves + 1,
                score = (state.score - 20).coerceAtLeast(0)
            )
        }
    }

    fun moveWasteToFoundation(state: SolitaireGameState, foundationIdx: Int): SolitaireGameState? {
        val card = state.waste.lastOrNull() ?: return null
        val targetFoundation = state.foundations.getOrNull(foundationIdx) ?: return null
        if (!card.canStackOnFoundation(targetFoundation.lastOrNull())) return null

        val newFoundations = state.foundations.mapIndexed { idx, pile ->
            if (idx == foundationIdx) pile + card else pile
        }
        return state.copy(
            waste = state.waste.dropLast(1),
            foundations = newFoundations,
            moves = state.moves + 1,
            score = state.score + 10
        )
    }

    fun moveWasteToTableau(state: SolitaireGameState, targetCol: Int): SolitaireGameState? {
        val card = state.waste.lastOrNull() ?: return null
        val destPile = state.tableau.getOrNull(targetCol) ?: return null
        if (!canPlaceOnTableau(card, destPile)) return null

        val newTableau = state.tableau.mapIndexed { idx, pile ->
            if (idx == targetCol) pile + card else pile
        }
        return state.copy(
            waste = state.waste.dropLast(1),
            tableau = newTableau,
            moves = state.moves + 1,
            score = state.score + 5
        )
    }

    fun moveTableauToFoundation(
        state: SolitaireGameState,
        sourceCol: Int,
        foundationIdx: Int
    ): SolitaireGameState? {
        val sourcePile = state.tableau.getOrNull(sourceCol) ?: return null
        val card = sourcePile.lastOrNull() ?: return null
        if (!card.isFaceUp) return null
        val targetFoundation = state.foundations.getOrNull(foundationIdx) ?: return null
        if (!card.canStackOnFoundation(targetFoundation.lastOrNull())) return null

        val remainingSource = flipTopIfNeeded(sourcePile.dropLast(1))
        val flippedBonus = if (remainingSource.size == sourcePile.size - 1 &&
            sourcePile.size >= 2 &&
            !sourcePile[sourcePile.lastIndex - 1].isFaceUp
        ) 5 else 0

        val newTableau = state.tableau.mapIndexed { idx, pile ->
            if (idx == sourceCol) remainingSource else pile
        }
        val newFoundations = state.foundations.mapIndexed { idx, pile ->
            if (idx == foundationIdx) pile + card else pile
        }
        return state.copy(
            tableau = newTableau,
            foundations = newFoundations,
            moves = state.moves + 1,
            score = state.score + 10 + flippedBonus
        )
    }

    fun moveTableauToTableau(
        state: SolitaireGameState,
        sourceCol: Int,
        cardIndex: Int,
        targetCol: Int
    ): SolitaireGameState? {
        if (sourceCol == targetCol) return null
        val sourcePile = state.tableau.getOrNull(sourceCol) ?: return null
        val destPile = state.tableau.getOrNull(targetCol) ?: return null
        if (cardIndex !in sourcePile.indices) return null

        val movingStack = sourcePile.subList(cardIndex, sourcePile.size)
        val leadCard = movingStack.firstOrNull() ?: return null
        if (!leadCard.isFaceUp) return null
        if (!canPlaceOnTableau(leadCard, destPile)) return null

        val wasUncoveringFaceDown = cardIndex > 0 && !sourcePile[cardIndex - 1].isFaceUp
        val newSource = flipTopIfNeeded(sourcePile.subList(0, cardIndex))

        val newTableau = state.tableau.mapIndexed { idx, pile ->
            when (idx) {
                sourceCol -> newSource
                targetCol -> pile + movingStack
                else -> pile
            }
        }
        return state.copy(
            tableau = newTableau,
            moves = state.moves + 1,
            score = state.score + if (wasUncoveringFaceDown) 5 else 0
        )
    }

    fun moveFoundationToTableau(
        state: SolitaireGameState,
        foundationIdx: Int,
        targetCol: Int
    ): SolitaireGameState? {
        val foundation = state.foundations.getOrNull(foundationIdx) ?: return null
        val card = foundation.lastOrNull() ?: return null
        val destPile = state.tableau.getOrNull(targetCol) ?: return null
        if (!canPlaceOnTableau(card, destPile)) return null

        val newFoundations = state.foundations.mapIndexed { idx, pile ->
            if (idx == foundationIdx) pile.dropLast(1) else pile
        }
        val newTableau = state.tableau.mapIndexed { idx, pile ->
            if (idx == targetCol) pile + card else pile
        }
        return state.copy(
            foundations = newFoundations,
            tableau = newTableau,
            moves = state.moves + 1,
            score = (state.score - 15).coerceAtLeast(0)
        )
    }

    fun smartMove(state: SolitaireGameState, selection: SolitaireSelection): SolitaireGameState? {
        when (selection) {
            is SolitaireSelection.Waste -> {
                for (f in 0..3) {
                    moveWasteToFoundation(state, f)?.let { return it }
                }
                for (col in 0..6) {
                    moveWasteToTableau(state, col)?.let { return it }
                }
            }
            is SolitaireSelection.Tableau -> {
                val pile = state.tableau.getOrNull(selection.col) ?: return null
                if (selection.cardIndex == pile.lastIndex) {
                    for (f in 0..3) {
                        moveTableauToFoundation(state, selection.col, f)?.let { return it }
                    }
                }
                // Prefer non-empty target columns first, then empty columns
                for (col in 0..6) {
                    if (state.tableau[col].isNotEmpty()) {
                        moveTableauToTableau(state, selection.col, selection.cardIndex, col)?.let { return it }
                    }
                }
                for (col in 0..6) {
                    if (state.tableau[col].isEmpty()) {
                        // Avoid pointless King shuffle from an already-bottom empty column
                        if (selection.cardIndex == 0 && pile[0].rank == Rank.KING) continue
                        moveTableauToTableau(state, selection.col, selection.cardIndex, col)?.let { return it }
                    }
                }
            }
            is SolitaireSelection.Foundation -> {
                for (col in 0..6) {
                    moveFoundationToTableau(state, selection.index, col)?.let { return it }
                }
            }
        }
        return null
    }

    fun canAutoComplete(state: SolitaireGameState): Boolean {
        if (state.isWon) return false
        if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) return false
        return state.tableau.all { pile -> pile.all { it.isFaceUp } }
    }

    fun stepAutoComplete(state: SolitaireGameState): SolitaireGameState? {
        // Move the lowest-rank top tableau card that fits on a foundation
        var bestCol = -1
        var bestFoundation = -1
        var lowestRank = Int.MAX_VALUE

        for (col in 0..6) {
            val top = state.tableau[col].lastOrNull() ?: continue
            for (f in 0..3) {
                if (top.canStackOnFoundation(state.foundations[f].lastOrNull())) {
                    if (top.rank.value < lowestRank) {
                        lowestRank = top.rank.value
                        bestCol = col
                        bestFoundation = f
                    }
                    break
                }
            }
        }

        return if (bestCol >= 0 && bestFoundation >= 0) {
            moveTableauToFoundation(state, bestCol, bestFoundation)
        } else {
            null
        }
    }

    fun findHint(state: SolitaireGameState): SolitaireHint? {
        // 1. Check Waste -> Foundation
        val wasteTop = state.waste.lastOrNull()
        if (wasteTop != null) {
            for (f in 0..3) {
                if (wasteTop.canStackOnFoundation(state.foundations[f].lastOrNull())) {
                    return SolitaireHint(
                        SolitaireSelection.Waste,
                        "Move ${wasteTop.rank.label}${wasteTop.suit.symbol} to Foundation"
                    )
                }
            }
        }
        // 2. Check Tableau top -> Foundation
        for (col in 0..6) {
            val pile = state.tableau[col]
            val top = pile.lastOrNull() ?: continue
            for (f in 0..3) {
                if (top.canStackOnFoundation(state.foundations[f].lastOrNull())) {
                    return SolitaireHint(
                        SolitaireSelection.Tableau(col, pile.lastIndex),
                        "Move ${top.rank.label}${top.suit.symbol} to Foundation"
                    )
                }
            }
        }
        // 3. Check Tableau -> Tableau that uncovers a face-down card or frees a column
        for (col in 0..6) {
            val pile = state.tableau[col]
            val firstFaceUpIdx = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx < 0) continue
            val lead = pile[firstFaceUpIdx]
            for (targetCol in 0..6) {
                if (targetCol == col) continue
                if (firstFaceUpIdx == 0 && lead.rank == Rank.KING && state.tableau[targetCol].isEmpty()) continue
                if (canPlaceOnTableau(lead, state.tableau[targetCol])) {
                    if (firstFaceUpIdx > 0 || (state.tableau[targetCol].isNotEmpty() && firstFaceUpIdx == 0)) {
                        return SolitaireHint(
                            SolitaireSelection.Tableau(col, firstFaceUpIdx),
                            "Move ${lead.rank.label}${lead.suit.symbol} to column ${targetCol + 1}"
                        )
                    }
                }
            }
        }
        // 4. Check Waste -> Tableau
        if (wasteTop != null) {
            for (col in 0..6) {
                if (canPlaceOnTableau(wasteTop, state.tableau[col])) {
                    return SolitaireHint(
                        SolitaireSelection.Waste,
                        "Move ${wasteTop.rank.label}${wasteTop.suit.symbol} to column ${col + 1}"
                    )
                }
            }
        }
        // 5. Stock draw
        if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
            return SolitaireHint(SolitaireSelection.Waste, "Draw from Stock")
        }
        return null
    }

    private fun canPlaceOnTableau(card: PlayingCard, destPile: List<PlayingCard>): Boolean {
        val top = destPile.lastOrNull()
        return if (top == null) {
            card.rank == Rank.KING
        } else {
            card.canStackOnTableau(top)
        }
    }

    private fun flipTopIfNeeded(pile: List<PlayingCard>): List<PlayingCard> {
        if (pile.isEmpty()) return pile
        val last = pile.last()
        return if (last.isFaceUp) {
            pile
        } else {
            pile.dropLast(1) + last.copy(isFaceUp = true)
        }
    }
}
