package com.example.solitaire

import com.example.cards.PlayingCard

enum class SolitaireDrawMode(val drawCount: Int, val label: String) {
    DRAW_1(1, "Draw 1"),
    DRAW_3(3, "Draw 3")
}

sealed interface SolitaireSelection {
    data object Waste : SolitaireSelection
    data class Tableau(val col: Int, val cardIndex: Int) : SolitaireSelection
    data class Foundation(val index: Int) : SolitaireSelection
}

data class SolitaireHint(
    val source: SolitaireSelection,
    val description: String
)

data class SolitaireGameState(
    val stock: List<PlayingCard> = emptyList(),
    val waste: List<PlayingCard> = emptyList(),
    val foundations: List<List<PlayingCard>> = List(4) { emptyList() },
    val tableau: List<List<PlayingCard>> = List(7) { emptyList() },
    val drawMode: SolitaireDrawMode = SolitaireDrawMode.DRAW_1,
    val moves: Int = 0,
    val score: Int = 0
) {
    val foundationCardCount: Int
        get() = foundations.sumOf { it.size }

    val remainingDeckCards: Int
        get() = 52 - foundationCardCount

    val isWon: Boolean
        get() = foundationCardCount == 52
}

data class SolitaireStats(
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
