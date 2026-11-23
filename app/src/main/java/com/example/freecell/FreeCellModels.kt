package com.example.freecell

import com.example.cards.PlayingCard

sealed interface FreeCellSelection {
    data class FreeCell(val index: Int) : FreeCellSelection
    data class Tableau(val col: Int, val cardIndex: Int) : FreeCellSelection
    data class Foundation(val index: Int) : FreeCellSelection
}

data class FreeCellHint(
    val source: FreeCellSelection,
    val description: String
)

data class FreeCellGameState(
    val dealNumber: Int = 1,
    val freeCells: List<PlayingCard?> = List(4) { null },
    val foundations: List<List<PlayingCard>> = List(4) { emptyList() },
    val tableau: List<List<PlayingCard>> = List(8) { emptyList() },
    val moves: Int = 0
) {
    val foundationCount: Int
        get() = foundations.sumOf { it.size }

    val remainingCards: Int
        get() = 52 - foundationCount

    val isWon: Boolean
        get() = foundationCount == 52
}

data class FreeCellStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestTimeSeconds: Int? = null,
    val bestMoves: Int? = null,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0
) {
    val winPercentage: Int
        get() = if (played > 0) (won * 100) / played else 0
}
