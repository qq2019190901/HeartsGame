package com.example.hearts.model

data class TrickCard(val player: Player, val card: Card)

class Trick(val leadPlayer: Player) {
    val cards: MutableList<TrickCard> = mutableListOf()
    val leadSuit: Suit? get() = cards.firstOrNull()?.card?.suit

    fun addCard(player: Player, card: Card) {
        cards.add(TrickCard(player, card))
    }

    val isComplete: Boolean get() = cards.size == 4

    fun winner(): Player {
        require(isComplete) { "Trick not complete" }
        val ls = leadSuit!!
        return cards.maxByOrNull { tc ->
            if (tc.card.suit == ls) tc.card.rank.value else 0
        }!!.player
    }

    fun allCards(): List<Card> = cards.map { it.card }
}
