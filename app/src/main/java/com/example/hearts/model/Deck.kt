package com.example.hearts.model

object Deck {
    val allCards: List<Card> by lazy {
        Suit.entries.flatMap { suit ->
            Rank.entries.map { rank -> Card(suit, rank) }
        }.sorted()
    }

    fun newShuffled(): List<Card> = allCards.shuffled()

    fun deal(shuffled: List<Card>): List<List<Card>> {
        require(shuffled.size == 52) { "Deck must have 52 cards" }
        return listOf(
            shuffled.subList(0, 13).sorted(),
            shuffled.subList(13, 26).sorted(),
            shuffled.subList(26, 39).sorted(),
            shuffled.subList(39, 52).sorted()
        )
    }
}
