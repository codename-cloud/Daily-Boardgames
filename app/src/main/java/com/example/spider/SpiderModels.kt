package com.example.spider

import com.example.cards.PlayingCard
import com.example.cards.Rank
import com.example.cards.Suit

enum class SpiderSuitMode(val suitCount: Int, val label: String) {
    ONE_SUIT(1, "1 Suit"),
    TWO_SUITS(2, "2 Suits"),
    FOUR_SUITS(4, "4 Suits")
}

data class SpiderCard(
    val uid: Int,
    val card: PlayingCard
) {
    val suit: Suit
        get() = card.suit

    val rank: Rank
        get() = card.rank

    val isFaceUp: Boolean
        get() = card.isFaceUp

    fun withFaceUp(faceUp: Boolean): SpiderCard = copy(card = card.copy(isFaceUp = faceUp))
}

data class SpiderSelection(
    val col: Int,
    val cardIndex: Int
)

data class SpiderGameState(
    val tableau: List<List<SpiderCard>> = List(10) { emptyList() },
    val stock: List<SpiderCard> = emptyList(),
    val completedSuits: List<Suit> = emptyList(),
    val suitMode: SpiderSuitMode = SpiderSuitMode.ONE_SUIT,
    val moves: Int = 0,
    val score: Int = 500
) {
    val stockDealsRemaining: Int
        get() = (stock.size + 9) / 10

    val isWon: Boolean
        get() = completedSuits.size == 8
}

data class SpiderStats(
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
