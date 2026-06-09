package com.example.hearts.model

enum class Suit(val symbol: String, val color: Int) {
    SPADES("\u2660", 0xFF000000.toInt()),
    HEARTS("\u2665", 0xFFE53935.toInt()),
    DIAMONDS("\u2666", 0xFFE53935.toInt()),
    CLUBS("\u2663", 0xFF000000.toInt())
}

enum class Rank(val symbol: String, val value: Int) {
    TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5),
    SIX("6", 6), SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9),
    TEN("10", 10), JACK("J", 11), QUEEN("Q", 12), KING("K", 13), ACE("A", 14)
}

data class Card(val suit: Suit, val rank: Rank) : Comparable<Card> {
    val isHeart: Boolean get() = suit == Suit.HEARTS
    val isQueenOfSpades: Boolean get() = suit == Suit.SPADES && rank == Rank.QUEEN

    val points: Int get() = when {
        isQueenOfSpades -> 13
        isHeart -> 1
        else -> 0
    }

    val displayName: String get() = "${rank.symbol}${suit.symbol}"

    override fun compareTo(other: Card): Int {
        val suitCompare = suit.ordinal.compareTo(other.suit.ordinal)
        return if (suitCompare != 0) suitCompare else rank.value.compareTo(other.rank.value)
    }

    override fun toString(): String = displayName

    companion object {
        fun compareBySuit(suit: Suit): Comparator<Card> = Comparator { a, b ->
            if (a.suit == suit && b.suit != suit) -1
            else if (a.suit != suit && b.suit == suit) 1
            else a.rank.value.compareTo(b.rank.value)
        }
    }
}
