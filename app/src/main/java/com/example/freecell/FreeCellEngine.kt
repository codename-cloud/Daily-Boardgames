package com.example.freecell

import com.example.cards.PlayingCard
import com.example.cards.createStandardDeck
import kotlin.random.Random

object FreeCellEngine {

    private fun dealFromSeed(dealNumber: Int): FreeCellGameState {
        val deck = createStandardDeck(seed = dealNumber.toLong(), faceUp = true)
        val tableau = MutableList(8) { mutableListOf<PlayingCard>() }
        deck.forEachIndexed { idx, card ->
            tableau[idx % 8].add(card)
        }
        return FreeCellGameState(
            dealNumber = dealNumber,
            freeCells = List(4) { null },
            foundations = List(4) { emptyList() },
            tableau = tableau.map { it.toList() },
            moves = 0
        )
    }

    fun newDeal(dealNumber: Int = Random.nextInt(1, 100_000)): FreeCellGameState {
        val base = ((dealNumber - 1).mod(99_999)) + 1
        for (attempt in 0 until 64) {
            val candidateNumber = ((base - 1 + attempt).mod(99_999)) + 1
            val deal = dealFromSeed(candidateNumber)
            if (FreeCellSolver.isWinnable(deal)) {
                return deal
            }
        }
        return dealFromSeed(1)
    }

    fun isOrderedRun(pile: List<PlayingCard>, startIndex: Int): Boolean {
        if (startIndex !in pile.indices) return false
        for (i in startIndex until pile.lastIndex) {
            val upper = pile[i]
            val lower = pile[i + 1]
            if (upper.isRed == lower.isRed || lower.rank.value != upper.rank.value - 1) {
                return false
            }
        }
        return true
    }

    fun maxMovableCards(state: FreeCellGameState, targetIsEmptyTableau: Boolean): Int {
        val emptyFree = state.freeCells.count { it == null }
        val emptyCols = state.tableau.count { it.isEmpty() }
        val usableEmptyCols = if (targetIsEmptyTableau) (emptyCols - 1).coerceAtLeast(0) else emptyCols
        return (1 + emptyFree) * (1 shl usableEmptyCols.coerceAtMost(4))
    }

    fun moveFreeCellToFoundation(
        state: FreeCellGameState,
        cellIdx: Int,
        foundationIdx: Int
    ): FreeCellGameState? {
        val card = state.freeCells.getOrNull(cellIdx) ?: return null
        val foundation = state.foundations.getOrNull(foundationIdx) ?: return null
        if (!card.canStackOnFoundation(foundation.lastOrNull())) return null

        return state.copy(
            freeCells = state.freeCells.mapIndexed { idx, c -> if (idx == cellIdx) null else c },
            foundations = state.foundations.mapIndexed { idx, pile -> if (idx == foundationIdx) pile + card else pile },
            moves = state.moves + 1
        )
    }

    fun moveFreeCellToTableau(
        state: FreeCellGameState,
        cellIdx: Int,
        targetCol: Int
    ): FreeCellGameState? {
        val card = state.freeCells.getOrNull(cellIdx) ?: return null
        val destPile = state.tableau.getOrNull(targetCol) ?: return null
        val destTop = destPile.lastOrNull()
        if (destTop != null && !card.canStackOnTableau(destTop)) return null

        return state.copy(
            freeCells = state.freeCells.mapIndexed { idx, c -> if (idx == cellIdx) null else c },
            tableau = state.tableau.mapIndexed { idx, pile -> if (idx == targetCol) pile + card else pile },
            moves = state.moves + 1
        )
    }

    fun moveFreeCellToFreeCell(
        state: FreeCellGameState,
        sourceIdx: Int,
        targetIdx: Int
    ): FreeCellGameState? {
        if (sourceIdx == targetIdx) return null
        val card = state.freeCells.getOrNull(sourceIdx) ?: return null
        if (state.freeCells.getOrNull(targetIdx) != null) return null

        return state.copy(
            freeCells = state.freeCells.mapIndexed { idx, c ->
                when (idx) {
                    sourceIdx -> null
                    targetIdx -> card
                    else -> c
                }
            },
            moves = state.moves + 1
        )
    }

    fun moveTableauToFreeCell(
        state: FreeCellGameState,
        sourceCol: Int,
        cellIdx: Int
    ): FreeCellGameState? {
        if (state.freeCells.getOrNull(cellIdx) != null) return null
        val sourcePile = state.tableau.getOrNull(sourceCol) ?: return null
        val card = sourcePile.lastOrNull() ?: return null

        return state.copy(
            freeCells = state.freeCells.mapIndexed { idx, c -> if (idx == cellIdx) card else c },
            tableau = state.tableau.mapIndexed { idx, pile -> if (idx == sourceCol) pile.dropLast(1) else pile },
            moves = state.moves + 1
        )
    }

    fun moveTableauToFoundation(
        state: FreeCellGameState,
        sourceCol: Int,
        foundationIdx: Int
    ): FreeCellGameState? {
        val sourcePile = state.tableau.getOrNull(sourceCol) ?: return null
        val card = sourcePile.lastOrNull() ?: return null
        val foundation = state.foundations.getOrNull(foundationIdx) ?: return null
        if (!card.canStackOnFoundation(foundation.lastOrNull())) return null

        return state.copy(
            foundations = state.foundations.mapIndexed { idx, pile -> if (idx == foundationIdx) pile + card else pile },
            tableau = state.tableau.mapIndexed { idx, pile -> if (idx == sourceCol) pile.dropLast(1) else pile },
            moves = state.moves + 1
        )
    }

    fun moveTableauToTableau(
        state: FreeCellGameState,
        sourceCol: Int,
        cardIndex: Int,
        targetCol: Int
    ): FreeCellGameState? {
        if (sourceCol == targetCol) return null
        val sourcePile = state.tableau.getOrNull(sourceCol) ?: return null
        val destPile = state.tableau.getOrNull(targetCol) ?: return null
        if (!isOrderedRun(sourcePile, cardIndex)) return null

        val movingStack = sourcePile.subList(cardIndex, sourcePile.size)
        val leadCard = movingStack.first()
        val destTop = destPile.lastOrNull()
        if (destTop != null && !leadCard.canStackOnTableau(destTop)) return null

        val capacity = maxMovableCards(state, targetIsEmptyTableau = destPile.isEmpty())
        if (movingStack.size > capacity) return null

        return state.copy(
            tableau = state.tableau.mapIndexed { idx, pile ->
                when (idx) {
                    sourceCol -> pile.subList(0, cardIndex)
                    targetCol -> pile + movingStack
                    else -> pile
                }
            },
            moves = state.moves + 1
        )
    }

    fun moveFoundationToTableau(
        state: FreeCellGameState,
        foundationIdx: Int,
        targetCol: Int
    ): FreeCellGameState? {
        val foundation = state.foundations.getOrNull(foundationIdx) ?: return null
        val card = foundation.lastOrNull() ?: return null
        val destPile = state.tableau.getOrNull(targetCol) ?: return null
        val destTop = destPile.lastOrNull()
        if (destTop != null && !card.canStackOnTableau(destTop)) return null

        return state.copy(
            foundations = state.foundations.mapIndexed { idx, pile -> if (idx == foundationIdx) pile.dropLast(1) else pile },
            tableau = state.tableau.mapIndexed { idx, pile -> if (idx == targetCol) pile + card else pile },
            moves = state.moves + 1
        )
    }

    fun smartMove(state: FreeCellGameState, selection: FreeCellSelection): FreeCellGameState? {
        when (selection) {
            is FreeCellSelection.FreeCell -> {
                for (f in 0..3) {
                    moveFreeCellToFoundation(state, selection.index, f)?.let { return it }
                }
                for (col in 0..7) {
                    if (state.tableau[col].isNotEmpty()) {
                        moveFreeCellToTableau(state, selection.index, col)?.let { return it }
                    }
                }
                for (col in 0..7) {
                    if (state.tableau[col].isEmpty()) {
                        moveFreeCellToTableau(state, selection.index, col)?.let { return it }
                    }
                }
            }
            is FreeCellSelection.Tableau -> {
                val pile = state.tableau.getOrNull(selection.col) ?: return null
                if (selection.cardIndex == pile.lastIndex) {
                    for (f in 0..3) {
                        moveTableauToFoundation(state, selection.col, f)?.let { return it }
                    }
                }
                for (col in 0..7) {
                    if (state.tableau[col].isNotEmpty()) {
                        moveTableauToTableau(state, selection.col, selection.cardIndex, col)?.let { return it }
                    }
                }
                if (selection.cardIndex == pile.lastIndex) {
                    val emptyFree = state.freeCells.indexOfFirst { it == null }
                    if (emptyFree >= 0) {
                        moveTableauToFreeCell(state, selection.col, emptyFree)?.let { return it }
                    }
                }
                for (col in 0..7) {
                    if (state.tableau[col].isEmpty()) {
                        moveTableauToTableau(state, selection.col, selection.cardIndex, col)?.let { return it }
                    }
                }
            }
            is FreeCellSelection.Foundation -> {
                for (col in 0..7) {
                    moveFoundationToTableau(state, selection.index, col)?.let { return it }
                }
            }
        }
        return null
    }

    /**
     * Checks if a card is provably safe to auto-move to a foundation:
     * rank <= 2 OR rank <= min(opposite-color foundation top rank) + 1
     */
    fun isSafeToAutoFoundation(state: FreeCellGameState, card: PlayingCard): Boolean {
        if (card.rank.value <= 2) return true
        val minOppositeRank = state.foundations
            .filterIndexed { _, pile ->
                val top = pile.lastOrNull()
                top == null || top.isRed != card.isRed
            }
            .minOfOrNull { it.lastOrNull()?.rank?.value ?: 0 } ?: 0
        // If we haven't even started an opposite-color foundation, rank 3 isn't safe yet unless minOpposite >= 2
        // Specifically: need both opposite-color suits to have at least rank - 1 (or rank - 2? Classic FreeCell rule:
        // safe if both opposite-color foundations have reached at least card.rank - 1, OR card.rank == 2)
        val oppositeRed = !card.isRed
        val oppositeSuitsCount = state.foundations.count { it.lastOrNull()?.isRed == oppositeRed }
        val minOpp = if (oppositeSuitsCount < 2) 0 else {
            state.foundations
                .filter { it.lastOrNull()?.isRed == oppositeRed }
                .minOf { it.last().rank.value }
        }
        return card.rank.value <= minOpp + 1 && minOppositeRank >= 0
    }

    fun stepSafeAutoFoundation(state: FreeCellGameState): FreeCellGameState? {
        for (i in 0..3) {
            val card = state.freeCells[i] ?: continue
            if (isSafeToAutoFoundation(state, card)) {
                for (f in 0..3) {
                    moveFreeCellToFoundation(state, i, f)?.let { return it }
                }
            }
        }
        for (col in 0..7) {
            val card = state.tableau[col].lastOrNull() ?: continue
            if (isSafeToAutoFoundation(state, card)) {
                for (f in 0..3) {
                    moveTableauToFoundation(state, col, f)?.let { return it }
                }
            }
        }
        return null
    }

    fun findHint(state: FreeCellGameState): FreeCellHint? {
        // 1. Foundation moves from tableau or free cells
        for (i in 0..3) {
            val card = state.freeCells[i] ?: continue
            for (f in 0..3) {
                if (card.canStackOnFoundation(state.foundations[f].lastOrNull())) {
                    return FreeCellHint(
                        FreeCellSelection.FreeCell(i),
                        "Move ${card.rank.label}${card.suit.symbol} to Foundation"
                    )
                }
            }
        }
        for (col in 0..7) {
            val pile = state.tableau[col]
            val top = pile.lastOrNull() ?: continue
            for (f in 0..3) {
                if (top.canStackOnFoundation(state.foundations[f].lastOrNull())) {
                    return FreeCellHint(
                        FreeCellSelection.Tableau(col, pile.lastIndex),
                        "Move ${top.rank.label}${top.suit.symbol} to Foundation"
                    )
                }
            }
        }
        // 2. FreeCell -> non-empty Tableau
        for (i in 0..3) {
            val card = state.freeCells[i] ?: continue
            for (col in 0..7) {
                val destTop = state.tableau[col].lastOrNull() ?: continue
                if (card.canStackOnTableau(destTop)) {
                    return FreeCellHint(
                        FreeCellSelection.FreeCell(i),
                        "Move ${card.rank.label}${card.suit.symbol} onto ${destTop.rank.label}${destTop.suit.symbol}"
                    )
                }
            }
        }
        // 3. Tableau run -> non-empty Tableau
        for (col in 0..7) {
            val pile = state.tableau[col]
            if (pile.isEmpty()) continue
            for (startIdx in pile.indices) {
                if (!isOrderedRun(pile, startIdx)) continue
                val lead = pile[startIdx]
                // Skip breaking an existing valid sequence unless it sits on another card or frees something
                if (startIdx > 0 && isOrderedRun(pile, startIdx - 1)) continue
                for (targetCol in 0..7) {
                    if (targetCol == col) continue
                    val destTop = state.tableau[targetCol].lastOrNull() ?: continue
                    if (lead.canStackOnTableau(destTop)) {
                        val capacity = maxMovableCards(state, targetIsEmptyTableau = false)
                        if (pile.size - startIdx <= capacity) {
                            return FreeCellHint(
                                FreeCellSelection.Tableau(col, startIdx),
                                "Move ${lead.rank.label}${lead.suit.symbol} onto ${destTop.rank.label}${destTop.suit.symbol}"
                            )
                        }
                    }
                }
            }
        }
        // 4. Move blocking card to empty FreeCell
        val emptyFree = state.freeCells.indexOfFirst { it == null }
        if (emptyFree >= 0) {
            for (col in 0..7) {
                val pile = state.tableau[col]
                if (pile.size >= 2) {
                    val top = pile.last()
                    return FreeCellHint(
                        FreeCellSelection.Tableau(col, pile.lastIndex),
                        "Move ${top.rank.label}${top.suit.symbol} to an open Free Cell"
                    )
                }
            }
        }
        return null
    }
}
