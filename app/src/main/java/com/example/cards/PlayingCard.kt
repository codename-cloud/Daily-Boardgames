package com.example.cards

import kotlin.random.Random

enum class Suit(
    val symbol: String,
    val isRed: Boolean,
    val displayName: String
) {
    SPADES("♠", false, "Spades"),
    HEARTS("♥", true, "Hearts"),
    CLUBS("♣", false, "Clubs"),
    DIAMONDS("♦", true, "Diamonds")
}

enum class Rank(
    val value: Int,
    val label: String
) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K")
}

data class PlayingCard(
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = true
) {
    val id: String
        get() = "${rank.value}_${suit.name}"

    val isRed: Boolean
        get() = suit.isRed

    fun canStackOnTableau(target: PlayingCard): Boolean {
        return target.isFaceUp && isRed != target.isRed && rank.value == target.rank.value - 1
    }

    fun canStackOnFoundation(topCard: PlayingCard?): Boolean {
        return if (topCard == null) {
            rank == Rank.ACE
        } else {
            suit == topCard.suit && rank.value == topCard.rank.value + 1
        }
    }
}

fun createStandardDeck(seed: Long? = null, faceUp: Boolean = true): List<PlayingCard> {
    val deck = mutableListOf<PlayingCard>()
    for (suit in Suit.entries) {
        for (rank in Rank.entries) {
            deck.add(PlayingCard(suit = suit, rank = rank, isFaceUp = faceUp))
        }
    }
    val rng = if (seed != null) Random(seed) else Random.Default
    return deck.shuffled(rng)
}
