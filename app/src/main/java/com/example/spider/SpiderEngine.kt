package com.example.spider

import com.example.cards.PlayingCard
import com.example.cards.Rank
import com.example.cards.Suit
import kotlin.random.Random

object SpiderEngine {

    fun createSpiderDeck(
        suitMode: SpiderSuitMode = SpiderSuitMode.ONE_SUIT,
        seed: Long? = null
    ): List<SpiderCard> {
        val suitsPattern = when (suitMode) {
            SpiderSuitMode.ONE_SUIT -> List(8) { Suit.SPADES }
            SpiderSuitMode.TWO_SUITS -> List(4) { Suit.SPADES } + List(4) { Suit.HEARTS }
            SpiderSuitMode.FOUR_SUITS -> List(2) { Suit.entries }.flatten()
        }
        val deck = ArrayList<SpiderCard>(104)
        var uid = 0
        for (suit in suitsPattern) {
            for (rank in Rank.entries) {
                deck.add(
                    SpiderCard(
                        uid = uid++,
                        card = PlayingCard(suit = suit, rank = rank, isFaceUp = false)
                    )
                )
            }
        }
        val rng = if (seed != null) Random(seed) else Random.Default
        return deck.shuffled(rng)
    }

    fun newDeal(
        suitMode: SpiderSuitMode = SpiderSuitMode.ONE_SUIT,
        seed: Long? = null
    ): SpiderGameState {
        val deck = createSpiderDeck(suitMode = suitMode, seed = seed)
        val tableau = MutableList(10) { mutableListOf<SpiderCard>() }
        var index = 0
        // Columns 0..3 receive 6 cards; columns 4..9 receive 5 cards (54 total)
        for (col in 0 until 10) {
            val count = if (col < 4) 6 else 5
            for (row in 0 until count) {
                val isTop = row == count - 1
                tableau[col].add(deck[index++].withFaceUp(isTop))
            }
        }
        val stock = deck.subList(index, deck.size).map { it.withFaceUp(false) }
        return SpiderGameState(
            tableau = tableau.map { it.toList() },
            stock = stock,
            completedSuits = emptyList(),
            suitMode = suitMode,
            moves = 0,
            score = 500
        )
    }

    /**
     * A movable Spider run must be all face-up, same suit, and descending by 1 rank per card.
     */
    fun isValidMovableRun(pile: List<SpiderCard>, startIndex: Int): Boolean {
        if (startIndex !in pile.indices) return false
        for (i in startIndex until pile.size) {
            if (!pile[i].isFaceUp) return false
        }
        for (i in startIndex until pile.lastIndex) {
            val current = pile[i]
            val next = pile[i + 1]
            if (current.suit != next.suit || next.rank.value != current.rank.value - 1) {
                return false
            }
        }
        return true
    }

    /**
     * Any single card or valid same-suit run can drop onto an empty column or onto a card
     * exactly one rank higher (of any suit).
     */
    fun canPlaceOnTableau(movingCard: SpiderCard, targetColumn: List<SpiderCard>): Boolean {
        if (targetColumn.isEmpty()) return true
        val top = targetColumn.last()
        return top.isFaceUp && movingCard.rank.value == top.rank.value - 1
    }

    fun moveTableauToTableau(
        state: SpiderGameState,
        sourceCol: Int,
        cardIndex: Int,
        targetCol: Int
    ): SpiderGameState? {
        if (sourceCol == targetCol) return null
        if (sourceCol !in state.tableau.indices || targetCol !in state.tableau.indices) return null
        val srcPile = state.tableau[sourceCol]
        if (!isValidMovableRun(srcPile, cardIndex)) return null

        val movingStack = srcPile.subList(cardIndex, srcPile.size)
        val dstPile = state.tableau[targetCol]
        if (!canPlaceOnTableau(movingStack.first(), dstPile)) return null

        val newSrc = srcPile.subList(0, cardIndex).toMutableList()
        if (newSrc.isNotEmpty() && !newSrc.last().isFaceUp) {
            newSrc[newSrc.lastIndex] = newSrc.last().withFaceUp(true)
        }
        val newDst = (dstPile + movingStack).toMutableList()

        val newTableau = state.tableau.toMutableList()
        newTableau[sourceCol] = newSrc.toList()
        newTableau[targetCol] = newDst.toList()

        return extractCompletedRuns(
            state.copy(
                tableau = newTableau.toList(),
                moves = state.moves + 1,
                score = (state.score - 1).coerceAtLeast(0)
            )
        )
    }

    fun smartMove(state: SpiderGameState, selection: SpiderSelection): SpiderGameState? {
        val srcPile = state.tableau.getOrNull(selection.col) ?: return null
        if (!isValidMovableRun(srcPile, selection.cardIndex)) return null
        val movingLead = srcPile[selection.cardIndex]

        // 1. Prefer matching-suit descending targets (extends same-suit run)
        val sameSuitTarget = state.tableau.indices.firstOrNull { col ->
            col != selection.col &&
                state.tableau[col].isNotEmpty() &&
                state.tableau[col].last().suit == movingLead.suit &&
                canPlaceOnTableau(movingLead, state.tableau[col])
        }
        if (sameSuitTarget != null) {
            return moveTableauToTableau(state, selection.col, selection.cardIndex, sameSuitTarget)
        }

        // 2. Next prefer any non-empty compatible column
        val anyNonEmptyTarget = state.tableau.indices.firstOrNull { col ->
            col != selection.col &&
                state.tableau[col].isNotEmpty() &&
                canPlaceOnTableau(movingLead, state.tableau[col])
        }
        if (anyNonEmptyTarget != null) {
            return moveTableauToTableau(state, selection.col, selection.cardIndex, anyNonEmptyTarget)
        }

        // 3. Finally allow moving to an empty column if it frees cards underneath
        if (selection.cardIndex > 0) {
            val emptyTarget = state.tableau.indices.firstOrNull { col ->
                col != selection.col && state.tableau[col].isEmpty()
            }
            if (emptyTarget != null) {
                return moveTableauToTableau(state, selection.col, selection.cardIndex, emptyTarget)
            }
        }
        return null
    }

    fun isStockDealBlockedByEmptyColumn(state: SpiderGameState): Boolean {
        if (state.stock.isEmpty()) return false
        val totalTableauCards = state.tableau.sumOf { it.size }
        return totalTableauCards >= state.tableau.size && state.tableau.any { it.isEmpty() }
    }

    fun canDealFromStock(state: SpiderGameState): Boolean {
        return state.stock.isNotEmpty() && !isStockDealBlockedByEmptyColumn(state)
    }

    fun dealFromStock(state: SpiderGameState): SpiderGameState? {
        if (!canDealFromStock(state)) return null
        val dealCount = minOf(state.tableau.size, state.stock.size)
        val dealtCards = state.stock.takeLast(dealCount).reversed()
        val remainingStock = state.stock.dropLast(dealCount)

        val newTableau = state.tableau.mapIndexed { col, pile ->
            if (col < dealtCards.size) {
                pile + dealtCards[col].withFaceUp(true)
            } else {
                pile
            }
        }

        return extractCompletedRuns(
            state.copy(
                tableau = newTableau,
                stock = remainingStock,
                moves = state.moves + 1,
                score = (state.score - 1).coerceAtLeast(0)
            )
        )
    }

    /**
     * Removes any completed King..Ace same-suit runs from the bottom of tableau columns,
     * exposes the newly uncovered card (if any), and awards +100 score per completed run.
     */
    fun extractCompletedRuns(state: SpiderGameState): SpiderGameState {
        val updatedTableau = state.tableau.toMutableList()
        val newlyCompleted = mutableListOf<Suit>()

        for (col in updatedTableau.indices) {
            var pile = updatedTableau[col]
            while (pile.size >= 13) {
                val tailStart = pile.size - 13
                val tail = pile.subList(tailStart, pile.size)
                val expectedSuit = tail.first().suit
                val isCompleteSuitRun = tail.indices.all { offset ->
                    val c = tail[offset]
                    c.isFaceUp &&
                        c.suit == expectedSuit &&
                        c.rank.value == Rank.KING.value - offset
                }
                if (!isCompleteSuitRun) break

                newlyCompleted.add(expectedSuit)
                val trimmed = pile.subList(0, tailStart).toMutableList()
                if (trimmed.isNotEmpty() && !trimmed.last().isFaceUp) {
                    trimmed[trimmed.lastIndex] = trimmed.last().withFaceUp(true)
                }
                pile = trimmed.toList()
            }
            updatedTableau[col] = pile
        }

        if (newlyCompleted.isEmpty()) return state

        return state.copy(
            tableau = updatedTableau.toList(),
            completedSuits = state.completedSuits + newlyCompleted,
            score = state.score + newlyCompleted.size * 100
        )
    }
}
