package com.example.hearts.engine

import com.example.hearts.model.*

enum class PassDirection { LEFT, RIGHT, ACROSS, NONE }

enum class GamePhase {
    PASSING,      // Selecting 3 cards to pass
    PLAYING,      // Playing tricks
    ROUND_END,    // Round finished, showing scores
    GAME_OVER     // Someone hit 100 points
}

data class GameState(
    val players: List<Player>,
    val currentPlayerIndex: Int,
    val currentTrick: Trick?,
    val tricksPlayed: Int,
    val heartsBroken: Boolean,
    val passDirection: PassDirection,
    val phase: GamePhase,
    val roundNumber: Int,
    val isFirstTrick: Boolean,
    val passedCards: Map<Player, List<Card>> = emptyMap(),
    val passCompleted: Boolean = false,
    val lastTrickWinner: Player? = null,
    val lastTrickCards: List<TrickCard> = emptyList()
)

class HeartsGame(
    humanPlayerName: String = "You"
) {
    val humanPlayer = Player(humanPlayerName, isHuman = true)
    val aiPlayers = listOf(
        Player("West", isHuman = false),
        Player("North", isHuman = false),
        Player("East", isHuman = false)
    )

    val allPlayers: List<Player> get() = listOf(humanPlayer) + aiPlayers

    private var roundNumber = 0
    private var passDirection = PassDirection.LEFT
    private var gameOver = false

    val state: GameState
        get() = GameState(
            players = allPlayers,
            currentPlayerIndex = currentPlayerIndex,
            currentTrick = currentTrick,
            tricksPlayed = tricksPlayed,
            heartsBroken = heartsBroken,
            passDirection = passDirection,
            phase = currentPhase,
            roundNumber = roundNumber,
            isFirstTrick = tricksPlayed == 0,
            passedCards = passedCards,
            passCompleted = passCompleted,
            lastTrickWinner = lastTrickWinner,
            lastTrickCards = lastTrickCards
        )

    private var currentPlayerIndex = 0
    private var currentTrick: Trick? = null
    private var tricksPlayed = 0
    private var heartsBroken = false
    private var currentPhase = GamePhase.PASSING
    private var passedCards: Map<Player, List<Card>> = emptyMap()
    private var passCompleted = false
    private var lastTrickWinner: Player? = null
    private var lastTrickCards: List<TrickCard> = emptyList()

    private val passRotation = listOf(
        PassDirection.LEFT,
        PassDirection.RIGHT,
        PassDirection.ACROSS,
        PassDirection.NONE
    )

    fun startNewRound() {
        allPlayers.forEach { it.resetForNewRound() }
        passDirection = passRotation[roundNumber % 4]
        roundNumber++
        tricksPlayed = 0
        heartsBroken = false
        currentTrick = null
        currentPhase = GamePhase.PASSING
        passCompleted = false
        passedCards = emptyMap()
        lastTrickWinner = null
        lastTrickCards = emptyList()

        val deck = Deck.newShuffled()
        val hands = Deck.deal(deck)
        allPlayers.forEachIndexed { i, player -> player.receiveCards(hands[i]) }

        if (passDirection == PassDirection.NONE) {
            passCompleted = true
            startPlaying()
        }
    }

    fun setAIPasses() {
        val passes = mutableMapOf<Player, List<Card>>()
        for (ai in aiPlayers) {
            passes[ai] = selectPassCards(ai)
        }
        passedCards = passes
    }

    fun humanPassSelected(cards: List<Card>) {
        require(cards.size == 3) { "Must pass exactly 3 cards" }
        require(cards.all { it in humanPlayer.hand }) { "Must own the cards" }
        val updated = passedCards.toMutableMap()
        updated[humanPlayer] = cards
        passedCards = updated
    }

    fun executePasses() {
        require(passedCards.size == 4) { "All players must select pass cards" }
        val passesToReceive = when (passDirection) {
            PassDirection.LEFT -> listOf(3, 0, 1, 2)
            PassDirection.RIGHT -> listOf(1, 2, 3, 0)
            PassDirection.ACROSS -> listOf(2, 3, 0, 1)
            PassDirection.NONE -> return
        }

        val cardsToReceive = allPlayers.map { player ->
            val receiverIdx = passesToReceive[allPlayers.indexOf(player)]
            passedCards[allPlayers[receiverIdx]]!!
        }

        allPlayers.forEachIndexed { i, player ->
            val sent = passedCards[player]!!
            player.hand.removeAll(sent)
            player.hand.addAll(cardsToReceive[i])
            player.hand.sort()
        }

        passCompleted = true
        startPlaying()
    }

    private fun startPlaying() {
        currentPhase = GamePhase.PLAYING
        currentPlayerIndex = allPlayers.indexOfFirst { it.hasTwoOfClubs() }
        if (currentPlayerIndex < 0) currentPlayerIndex = 0
        currentTrick = Trick(allPlayers[currentPlayerIndex])
    }

    fun playCard(player: Player, card: Card): Boolean {
        require(currentPhase == GamePhase.PLAYING) { "Not in playing phase" }
        require(currentTrick != null) { "No active trick" }
        require(allPlayers[currentPlayerIndex] == player) { "Not your turn" }

        val leadSuit = currentTrick!!.leadSuit
        val validCards = player.validPlays(leadSuit, heartsBroken, isFirstTrick = tricksPlayed == 0)
        require(card in validCards) { "Invalid card: $card. Valid: $validCards" }

        player.playCard(card)
        currentTrick!!.addCard(player, card)

        if (!heartsBroken && card.isHeart) {
            heartsBroken = true
        }

        if (currentTrick!!.isComplete) {
            finishTrick()
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4
        }

        return true
    }

    private fun finishTrick() {
        val winner = currentTrick!!.winner()
        val trickCards = currentTrick!!.cards.toList()
        val allCards = currentTrick!!.allCards()
        winner.takeTrick(allCards)
        tricksPlayed++

        if (!heartsBroken && allCards.any { it.isHeart }) {
            heartsBroken = true
        }

        lastTrickWinner = winner
        lastTrickCards = trickCards

        if (tricksPlayed >= 13) {
            endRound()
        } else {
            currentPlayerIndex = allPlayers.indexOf(winner)
            currentTrick = Trick(winner)
        }
    }

    private fun endRound() {
        currentPhase = GamePhase.ROUND_END
        currentTrick = null

        val moonShooter = allPlayers.find { p ->
            p.takenCards.size == 0 || (p.takenCards.all { it.isHeart || it.isQueenOfSpades } &&
                p.takenCards.count { it.isHeart } == 13 &&
                p.takenCards.any { it.isQueenOfSpades })
        }

        if (moonShooter != null) {
            val shotMoon = moonShooter.takenCards.count { it.isHeart } == 13 &&
                    moonShooter.takenCards.any { it.isQueenOfSpades }
            if (shotMoon) {
                moonShooter.takenCards.clear()
                allPlayers.filter { it != moonShooter }.forEach { p ->
                    p.takenCards.addAll(Deck.allCards.filter { it.isHeart || it.isQueenOfSpades })
                }
            }
        }

        allPlayers.forEach { it.addToTotalScore() }

        if (allPlayers.any { it.totalScore >= 100 }) {
            currentPhase = GamePhase.GAME_OVER
            gameOver = true
        }
    }

    fun isGameOver(): Boolean = gameOver

    private fun selectPassCards(player: Player): List<Card> {
        val hand = player.hand.toMutableList()

        val highHearts = hand.filter { it.isHeart && it.rank.value >= Rank.QUEEN.value }
        val queenOfSpades = hand.filter { it.isQueenOfSpades }
        val highSpades = hand.filter { it.suit == Suit.SPADES && it.rank.value >= Rank.KING.value && !it.isQueenOfSpades }
        val highCards = hand.filter { it.rank.value >= Rank.KING.value && !it.isHeart && !it.isQueenOfSpades && it.suit != Suit.SPADES }

        val passList = mutableListOf<Card>()

        passList.addAll(queenOfSpades.take(1))
        passList.addAll(highHearts.take(3 - passList.size))
        passList.addAll(highSpades.take(3 - passList.size))
        passList.addAll(highCards.take(3 - passList.size))

        if (passList.size < 3) {
            val remaining = hand.filter { it !in passList }
                .sortedByDescending { it.rank.value }
            passList.addAll(remaining.take(3 - passList.size))
        }

        return passList.take(3)
    }

    fun findStartingPlayer(): Player = allPlayers[currentPlayerIndex]

    fun getCurrentPlayer(): Player = allPlayers[currentPlayerIndex]
}
