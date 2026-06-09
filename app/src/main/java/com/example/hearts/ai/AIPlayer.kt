package com.example.hearts.ai

import com.example.hearts.model.*

object AIPlayer {
    fun chooseCard(
        player: Player,
        leadSuit: Suit?,
        heartsBroken: Boolean,
        isFirstTrick: Boolean,
        trickCards: List<TrickCard> = emptyList()
    ): Card {
        val hand = player.hand
        if (hand.isEmpty()) error("No cards in hand")

        if (leadSuit == null) {
            return chooseLead(player, heartsBroken, isFirstTrick)
        }

        return chooseFollow(player, leadSuit, isFirstTrick, trickCards)
    }

    private fun chooseLead(player: Player, heartsBroken: Boolean, isFirstTrick: Boolean): Card {
        val hand = player.hand

        if (isFirstTrick) {
            return hand.find { it.suit == Suit.CLUBS && it.rank == Rank.TWO }!!
        }

        val safeCards = hand.filter { it.suit != Suit.HEARTS || heartsBroken }
            .filter { it.suit != Suit.SPADES || it.rank != Rank.QUEEN }

        if (safeCards.isEmpty()) {
            return hand.first()
        }

        val shortSuits = safeCards.groupBy { it.suit }
            .filter { it.value.size <= 3 }
            .flatMap { it.value }

        val candidates = if (shortSuits.isNotEmpty()) shortSuits else safeCards

        val noAceOrKing = candidates.filter { it.rank.value < Rank.ACE.value }

        val chosen = if (noAceOrKing.isNotEmpty()) {
            noAceOrKing.minByOrNull { it.rank.value } ?: candidates.first()
        } else {
            candidates.minByOrNull { it.rank.value } ?: candidates.first()
        }

        return chosen
    }

    private fun chooseFollow(
        player: Player,
        leadSuit: Suit,
        isFirstTrick: Boolean,
        trickCards: List<TrickCard>
    ): Card {
        val hand = player.hand
        val suitCards = hand.filter { it.suit == leadSuit }

        if (suitCards.isNotEmpty()) {
            return followSuit(suitCards, leadSuit, trickCards, isFirstTrick)
        }

        return discardOffSuit(hand, leadSuit, isFirstTrick)
    }

    private fun followSuit(
        suitCards: List<Card>,
        leadSuit: Suit,
        trickCards: List<TrickCard>,
        isFirstTrick: Boolean
    ): Card {
        val highestInTrick = trickCards
            .filter { it.card.suit == leadSuit }
            .maxOfOrNull { it.card.rank.value } ?: 0

        val lowerCards = suitCards.filter { it.rank.value < highestInTrick }
        if (lowerCards.isNotEmpty()) {
            return lowerCards.maxByOrNull { it.rank.value }!!
        }

        val higherCards = suitCards.filter { it.rank.value > highestInTrick }
        if (higherCards.isNotEmpty()) {
            val safeHigh = higherCards.filter { !it.isHeart && !it.isQueenOfSpades }
            if (safeHigh.isNotEmpty()) {
                return safeHigh.minByOrNull { it.rank.value }!!
            }
            return higherCards.minByOrNull { it.rank.value }!!
        }

        return suitCards.first()
    }

    private fun discardOffSuit(hand: List<Card>, leadSuit: Suit, isFirstTrick: Boolean): Card {
        if (isFirstTrick) {
            val safe = hand.filter { !it.isHeart && !it.isQueenOfSpades }
            if (safe.isNotEmpty()) {
                return safe.maxByOrNull { it.rank.value }!!
            }
        }

        val queenOfSpades = hand.find { it.isQueenOfSpades }
        if (queenOfSpades != null) return queenOfSpades

        val highHearts = hand.filter { it.isHeart && it.rank.value >= Rank.KING.value }
        if (highHearts.isNotEmpty()) {
            return highHearts.maxByOrNull { it.rank.value }!!
        }

        val lowHearts = hand.filter { it.isHeart && it.rank.value <= Rank.FIVE.value }
        if (lowHearts.isNotEmpty()) {
            return lowHearts.minByOrNull { it.rank.value }!!
        }

        val voidSuits = hand.groupBy { it.suit }
            .filter { it.value.size <= 2 }
            .flatMap { it.value }

        if (voidSuits.isNotEmpty()) {
            return voidSuits.maxByOrNull { it.rank.value }!!
        }

        return hand.maxByOrNull { it.rank.value }!!
    }
}
