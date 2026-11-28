package com.example

import com.example.cards.PlayingCard
import com.example.cards.Rank
import com.example.cards.Suit
import com.example.freecell.FreeCellEngine
import com.example.freecell.FreeCellGameState
import com.example.freecell.FreeCellSolver
import com.example.solitaire.SolitaireDrawMode
import com.example.solitaire.SolitaireEngine
import com.example.solitaire.SolitaireGameState
import com.example.solitaire.SolitaireSolver
import com.example.spider.SpiderCard
import com.example.spider.SpiderEngine
import com.example.spider.SpiderGameState
import com.example.spider.SpiderSuitMode
import com.example.sudoku.SudokuDifficulty
import com.example.sudoku.SudokuGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardgamesEngineTest {

    @Test
    fun testSolitaireInitialDeal() {
        val deal = SolitaireEngine.newDeal(SolitaireDrawMode.DRAW_1, seed = 42L)
        assertEquals(24, deal.stock.size)
        assertTrue(deal.waste.isEmpty())
        assertEquals(7, deal.tableau.size)
        for (col in 0 until 7) {
            assertEquals(col + 1, deal.tableau[col].size)
            assertTrue(deal.tableau[col].last().isFaceUp)
            for (i in 0 until col) {
                assertFalse(deal.tableau[col][i].isFaceUp)
            }
        }
    }

    @Test
    fun testSolitaireDraw3AndRecycle() {
        var state = SolitaireEngine.newDeal(SolitaireDrawMode.DRAW_3, seed = 7L)
        state = SolitaireEngine.drawFromStock(state)!!
        assertEquals(21, state.stock.size)
        assertEquals(3, state.waste.size)
        assertTrue(state.waste.all { it.isFaceUp })

        // Draw remaining 7 times (21 cards)
        repeat(7) {
            state = SolitaireEngine.drawFromStock(state)!!
        }
        assertEquals(0, state.stock.size)
        assertEquals(24, state.waste.size)

        // Next draw recycles waste back to stock
        state = SolitaireEngine.drawFromStock(state)!!
        assertEquals(24, state.stock.size)
        assertEquals(0, state.waste.size)
    }

    @Test
    fun testSolitaireAutoCompleteDetection() {
        val tableau = List(7) { col ->
            if (col == 0) listOf(PlayingCard(Suit.SPADES, Rank.ACE, isFaceUp = true))
            else emptyList()
        }
        val state = SolitaireGameState(tableau = tableau)
        assertTrue(SolitaireEngine.canAutoComplete(state))
        val stepped = SolitaireEngine.stepAutoComplete(state)
        assertNotNull(stepped)
        assertEquals(1, stepped!!.foundations[0].size)
    }

    @Test
    fun testFreeCellInitialDealAndSupermoveCapacity() {
        val deal = FreeCellEngine.newDeal(dealNumber = 123)
        assertEquals(4, deal.freeCells.size)
        assertTrue(deal.freeCells.all { it == null })
        assertEquals(8, deal.tableau.size)
        assertEquals(52, deal.tableau.sumOf { it.size })
        // 4 empty free cells, 0 empty tableau cols => (1 + 4) * 2^0 = 5
        assertEquals(5, FreeCellEngine.maxMovableCards(deal, targetIsEmptyTableau = false))
    }

    @Test
    fun testFreeCellOrderedRunAndMove() {
        val state = FreeCellGameState(
            tableau = listOf(
                listOf(
                    PlayingCard(Suit.SPADES, Rank.SIX),
                    PlayingCard(Suit.HEARTS, Rank.FIVE)
                ),
                listOf(PlayingCard(Suit.DIAMONDS, Rank.SEVEN)),
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
            )
        )
        assertTrue(FreeCellEngine.isOrderedRun(state.tableau[0], 0))
        val moved = FreeCellEngine.moveTableauToTableau(state, sourceCol = 0, cardIndex = 0, targetCol = 1)
        assertNotNull(moved)
        assertTrue(moved!!.tableau[0].isEmpty())
        assertEquals(3, moved.tableau[1].size)

        // Invalid same-color move rejected
        val invalid = FreeCellEngine.moveTableauToTableau(state, sourceCol = 1, cardIndex = 0, targetCol = 0)
        assertNull(invalid)
    }

    @Test
    fun testSudokuGeneratorProducesUniqueValidPuzzles() {
        for (diff in listOf(SudokuDifficulty.EASY, SudokuDifficulty.MEDIUM, SudokuDifficulty.HARD)) {
            val board = SudokuGenerator.generatePuzzle(diff, seed = 99L)
            assertEquals(9, board.size)
            assertTrue(board.all { it.size == 9 })

            val rawGrid = Array(9) { r -> IntArray(9) { c -> board[r][c].value } }
            assertEquals(1, SudokuGenerator.countSolutions(rawGrid, limit = 2))

            // Verify solution itself has no row/col/box duplicates
            for (r in 0 until 9) {
                val rowVals = board[r].map { it.solution }.toSet()
                assertEquals(9, rowVals.size)
            }
            for (c in 0 until 9) {
                val colVals = (0 until 9).map { r -> board[r][c].solution }.toSet()
                assertEquals(9, colVals.size)
            }
        }
    }

    @Test
    fun testAllGeneratedSolitaireDealsAreWinnable() {
        val startMs = System.currentTimeMillis()
        for (seed in 1L..25L) {
            val draw1 = SolitaireEngine.newDeal(SolitaireDrawMode.DRAW_1, seed = seed)
            assertEquals(52, draw1.stock.size + draw1.tableau.sumOf { it.size })
            assertTrue("Draw-1 deal for seed $seed should be winnable", SolitaireSolver.isWinnable(draw1))

            val draw3 = SolitaireEngine.newDeal(SolitaireDrawMode.DRAW_3, seed = seed)
            assertEquals(52, draw3.stock.size + draw3.tableau.sumOf { it.size })
            assertTrue("Draw-3 deal for seed $seed should be winnable", SolitaireSolver.isWinnable(draw3))
        }
        val elapsed = System.currentTimeMillis() - startMs
        println("Generated and verified 50 Solitaire deals (25 Draw-1 + 25 Draw-3) in ${elapsed}ms")
    }

    @Test
    fun testAllGeneratedFreeCellDealsAreWinnable() {
        val startMs = System.currentTimeMillis()
        for (dealNum in 1..25) {
            val deal = FreeCellEngine.newDeal(dealNumber = dealNum)
            assertEquals(52, deal.tableau.sumOf { it.size })
            assertTrue("FreeCell deal #${deal.dealNumber} should be winnable", FreeCellSolver.isWinnable(deal))
        }
        val elapsed = System.currentTimeMillis() - startMs
        println("Generated and verified 25 FreeCell deals in ${elapsed}ms")
    }

    @Test
    fun testSpiderInitialDealAndSuitModes() {
        val oneSuit = SpiderEngine.newDeal(SpiderSuitMode.ONE_SUIT, seed = 11L)
        assertEquals(50, oneSuit.stock.size)
        assertEquals(5, oneSuit.stockDealsRemaining)
        assertEquals(10, oneSuit.tableau.size)
        assertEquals(54, oneSuit.tableau.sumOf { it.size })
        for (col in 0 until 10) {
            val expectedCount = if (col < 4) 6 else 5
            assertEquals(expectedCount, oneSuit.tableau[col].size)
            assertTrue(oneSuit.tableau[col].last().isFaceUp)
            assertTrue(oneSuit.tableau[col].dropLast(1).none { it.isFaceUp })
        }
        val allOneSuitCards = oneSuit.stock + oneSuit.tableau.flatten()
        assertEquals(104, allOneSuitCards.map { it.uid }.toSet().size)
        assertTrue(allOneSuitCards.all { it.suit == Suit.SPADES })

        val twoSuits = SpiderEngine.createSpiderDeck(SpiderSuitMode.TWO_SUITS, seed = 11L)
        assertEquals(setOf(Suit.SPADES, Suit.HEARTS), twoSuits.map { it.suit }.toSet())

        val fourSuits = SpiderEngine.createSpiderDeck(SpiderSuitMode.FOUR_SUITS, seed = 11L)
        assertEquals(Suit.entries.toSet(), fourSuits.map { it.suit }.toSet())
    }

    @Test
    fun testSpiderRunRulesStockGuardAndSuitRunCompletion() {
        var uid = 0
        fun card(suit: Suit, rank: Rank, faceUp: Boolean = true) =
            SpiderCard(uid = uid++, card = PlayingCard(suit = suit, rank = rank, isFaceUp = faceUp))

        // Mixed-suit descending sequence cannot move together, only bottom card can move
        val mixedCol = listOf(
            card(Suit.SPADES, Rank.NINE),
            card(Suit.HEARTS, Rank.EIGHT),
            card(Suit.HEARTS, Rank.SEVEN)
        )
        assertFalse(SpiderEngine.isValidMovableRun(mixedCol, 0))
        assertTrue(SpiderEngine.isValidMovableRun(mixedCol, 1))

        val initialState = SpiderGameState(
            tableau = listOf(
                mixedCol,
                listOf(card(Suit.SPADES, Rank.NINE)),
                List(6) { card(Suit.SPADES, Rank.THREE) },
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
            ),
            stock = List(10) { card(Suit.SPADES, Rank.TWO, faceUp = false) }
        )

        // Stock deal blocked when an empty column exists and >= 10 cards are on tableau
        assertTrue(SpiderEngine.isStockDealBlockedByEmptyColumn(initialState))
        assertNull(SpiderEngine.dealFromStock(initialState))

        // Same-suit sub-run (8H, 7H) can drop onto any suit 9 (9S)
        val moved = SpiderEngine.moveTableauToTableau(initialState, sourceCol = 0, cardIndex = 1, targetCol = 1)
        assertNotNull(moved)
        assertEquals(1, moved!!.tableau[0].size)
        assertEquals(3, moved.tableau[1].size)

        // Completing a K..A same-suit run automatically clears it and reveals underlying card
        val nearlyFullRun = Rank.entries.sortedByDescending { it.value }.dropLast(1).map { r ->
            card(Suit.SPADES, r, faceUp = true)
        }
        val stateBeforeComplete = SpiderGameState(
            tableau = listOf(
                listOf(card(Suit.HEARTS, Rank.FOUR, faceUp = false)) + nearlyFullRun,
                listOf(card(Suit.SPADES, Rank.ACE, faceUp = true)),
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
            ),
            completedSuits = List(7) { Suit.SPADES }
        )
        val completedState = SpiderEngine.moveTableauToTableau(stateBeforeComplete, sourceCol = 1, cardIndex = 0, targetCol = 0)
        assertNotNull(completedState)
        assertEquals(8, completedState!!.completedSuits.size)
        assertTrue(completedState.isWon)
        assertEquals(1, completedState.tableau[0].size)
        assertTrue(completedState.tableau[0].first().isFaceUp)
        assertEquals(Rank.FOUR, completedState.tableau[0].first().rank)
    }
}
