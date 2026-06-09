package com.example.hearts.model

class Player(
    val name: String,
    val isHuman: Boolean = false
) {
    val hand: MutableList<Card> = mutableListOf()
    val takenCards: MutableList<Card> = mutableListOf()
    var roundScore: Int = 0
        private set
    var totalScore: Int = 0
        private set

    fun receiveCards(cards: List<Card>) {
        hand.clear()
        hand.addAll(cards)
    }

    fun playCard(card: Card): Card {
        require(card in hand) { "Card $card not in hand" }
        hand.remove(card)
        return card
    }

    fun takeTrick(cards: List<Card>) {
        takenCards.addAll(cards)
        roundScore = takenCards.sumOf { it.points }
    }

    fun addToTotalScore() {
        totalScore += roundScore
    }

    fun resetForNewRound() {
        hand.clear()
        takenCards.clear()
        roundScore = 0
    }

    fun hasSuit(suit: Suit): Boolean = hand.any { it.suit == suit }

    fun hasOnlyHearts(): Boolean = hand.all { it.suit == Suit.HEARTS }

    fun validPlays(leadSuit: Suit?, heartsBroken: Boolean, isFirstTrick: Boolean): List<Card> {
        if (hand.isEmpty()) return emptyList()

        if (leadSuit == null) {
            if (isFirstTrick) {
                val twoOfClubs = hand.find { it.suit == Suit.CLUBS && it.rank == Rank.TWO }
                if (twoOfClubs != null) return listOf(twoOfClubs)
            }
            if (!heartsBroken && hand.any { it.suit != Suit.HEARTS }) {
                return hand.filter { it.suit != Suit.HEARTS }
            }
            return hand.toList()
        }

        val suitCards = hand.filter { it.suit == leadSuit }
        if (suitCards.isNotEmpty()) {
            if (isFirstTrick) {
                val withoutPenalty = suitCards.filter { !it.isHeart && !it.isQueenOfSpades }
                if (withoutPenalty.isNotEmpty()) return withoutPenalty
            }
            return suitCards
        }

        if (isFirstTrick) {
            val safe = hand.filter { !it.isHeart && !it.isQueenOfSpades }
            if (safe.isNotEmpty()) return safe
        }

        return hand.toList()
    }

    fun hasTwoOfClubs(): Boolean = hand.any { it.suit == Suit.CLUBS && it.rank == Rank.TWO }
}
