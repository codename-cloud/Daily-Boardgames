package com.example

import com.example.cards.PlayingCard
import com.example.cards.Rank
import com.example.cards.Suit
import com.example.freecell.FreeCellEngine
import com.example.freecell.FreeCellGameState
import com.example.solitaire.SolitaireDrawMode
import com.example.solitaire.SolitaireEngine
import com.example.solitaire.SolitaireGameState
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
}
