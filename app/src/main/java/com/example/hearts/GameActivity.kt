package com.example.hearts

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.hearts.ai.AIPlayer
import com.example.hearts.engine.GamePhase
import com.example.hearts.engine.HeartsGame
import com.example.hearts.engine.PassDirection
import com.example.hearts.model.Card
import com.example.hearts.model.Player
import com.example.hearts.model.TrickCard
import com.example.hearts.ui.HandView

class GameActivity : AppCompatActivity() {

    private lateinit var game: HeartsGame
    private lateinit var handView: HandView
    private lateinit var trickArea: LinearLayout
    private lateinit var phaseText: TextView
    private lateinit var scoreText: TextView
    private lateinit var actionButton: Button
    private lateinit var passCardsContainer: LinearLayout
    private lateinit var passCardsText: TextView
    private lateinit var mainContainer: FrameLayout

    private val handler = Handler(Looper.getMainLooper())
    private var selectedPassCards = mutableListOf<Card>()

    private val playerNameViews = mutableMapOf<Player, TextView>()
    private val playerScoreViews = mutableMapOf<Player, TextView>()
    private val playerCardViews = mutableMapOf<Player, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        game = HeartsGame()

        handView = findViewById(R.id.hand_view)
        trickArea = findViewById(R.id.trick_area)
        phaseText = findViewById(R.id.phase_text)
        scoreText = findViewById(R.id.score_text)
        actionButton = findViewById(R.id.action_button)
        passCardsContainer = findViewById(R.id.pass_cards_container)
        passCardsText = findViewById(R.id.pass_cards_text)
        mainContainer = findViewById(R.id.main_container)

        initPlayerInfoViews()
        setupListeners()
        startNewRound()
    }

    private fun initPlayerInfoViews() {
        val playerLayouts = listOf(
            findViewById<LinearLayout>(R.id.player_top_layout),
            findViewById<LinearLayout>(R.id.player_left_layout),
            findViewById<LinearLayout>(R.id.player_right_layout)
        )

        val aiPlayers = game.aiPlayers
        for (i in aiPlayers.indices) {
            val layout = playerLayouts[i]
            val nameView = layout.findViewById<TextView>(R.id.player_name)
            val scoreView = layout.findViewById<TextView>(R.id.player_score)
            val cardView = layout.findViewById<TextView>(R.id.player_card)
            nameView.text = aiPlayers[i].name
            playerNameViews[aiPlayers[i]] = nameView
            playerScoreViews[aiPlayers[i]] = scoreView
            playerCardViews[aiPlayers[i]] = cardView
        }
    }

    private fun setupListeners() {
        handView.onCardSelected = { card ->
            val state = game.state
            when (state.phase) {
                GamePhase.PASSING -> {
                    if (card in selectedPassCards) {
                        selectedPassCards.remove(card)
                        handView.clearSelection()
                    } else {
                        if (selectedPassCards.size >= 3) {
                            handView.clearSelection()
                            selectedPassCards.clear()
                        }
                        selectedPassCards.add(card)
                    }
                    updatePassUI()
                }
                GamePhase.PLAYING -> {
                    if (state.currentPlayerIndex == 0) {
                        actionButton.isEnabled = true
                    }
                }
                else -> {}
            }
        }

        actionButton.setOnClickListener {
            val state = game.state
            when (state.phase) {
                GamePhase.PASSING -> confirmPass()
                GamePhase.PLAYING -> playSelectedCard()
                GamePhase.ROUND_END -> startNewRound()
                GamePhase.GAME_OVER -> finish()
            }
        }
    }

    private fun startNewRound() {
        game.startNewRound()
        updateAllUI()
    }

    private fun updateAllUI() {
        val state = game.state
        updateScoreDisplay()
        updatePhaseDisplay()
        hideAllPlayerCards()

        when (state.phase) {
            GamePhase.PASSING -> setupPassingPhase()
            GamePhase.PLAYING -> setupPlayingPhase()
            GamePhase.ROUND_END -> showRoundEnd()
            GamePhase.GAME_OVER -> showGameOver()
        }
    }

    private fun updateScoreDisplay() {
        val sb = StringBuilder()
        for (p in game.allPlayers) {
            val name = if (p.isHuman) "你" else p.name
            val marker = if (game.state.currentPlayerIndex == game.allPlayers.indexOf(p) && game.state.phase == GamePhase.PLAYING) " ▶" else ""
            sb.append("$name: ${p.totalScore}$marker  ")
        }
        scoreText.text = sb.toString().trim()

        for (p in game.allPlayers) {
            playerScoreViews[p]?.text = "${p.totalScore}分"
            playerNameViews[p]?.text = if (p.isHuman) "你" else p.name
        }
    }

    private fun updatePhaseDisplay() {
        val state = game.state
        phaseText.text = when (state.phase) {
            GamePhase.PASSING -> {
                val dir = when (state.passDirection) {
                    PassDirection.LEFT -> "向左传"
                    PassDirection.RIGHT -> "向右传"
                    PassDirection.ACROSS -> "对面传"
                    PassDirection.NONE -> "不传牌"
                }
                "第${state.roundNumber}轮 - 传牌阶段 ($dir)"
            }
            GamePhase.PLAYING -> "第${state.roundNumber}轮 - 第${state.tricksPlayed + 1}/13墩"
            GamePhase.ROUND_END -> "第${state.roundNumber}轮结束"
            GamePhase.GAME_OVER -> "游戏结束!"
        }
    }

    private fun hideAllPlayerCards() {
        for (p in game.allPlayers) {
            playerCardViews[p]?.text = ""
        }
    }

    private fun setupPassingPhase() {
        selectedPassCards.clear()
        passCardsContainer.visibility = View.VISIBLE
        val dirText = when (game.state.passDirection) {
            PassDirection.LEFT -> "请选择3张牌传给 左边玩家"
            PassDirection.RIGHT -> "请选择3张牌传给 右边玩家"
            PassDirection.ACROSS -> "请选择3张牌传给 对面玩家"
            PassDirection.NONE -> "本轮不传牌"
        }
        passCardsText.text = dirText
        actionButton.text = "确认传牌"
        actionButton.isEnabled = false
        actionButton.visibility = View.VISIBLE

        game.setAIPasses()

        handView.setCards(game.humanPlayer.hand.toList())
    }

    private fun updatePassUI() {
        passCardsText.text = when {
            selectedPassCards.size == 0 -> {
                val dirText = when (game.state.passDirection) {
                    PassDirection.LEFT -> "请选择3张牌传给 左边玩家"
                    PassDirection.RIGHT -> "请选择3张牌传给 右边玩家"
                    PassDirection.ACROSS -> "请选择3张牌传给 对面玩家"
                    PassDirection.NONE -> "本轮不传牌"
                }
                dirText
            }
            selectedPassCards.size < 3 -> "已选 ${selectedPassCards.size}/3: ${selectedPassCards.joinToString(" ")}"
            else -> "已选 3/3: ${selectedPassCards.joinToString(" ")} - 点击确认"
        }
        actionButton.isEnabled = selectedPassCards.size == 3
    }

    private fun confirmPass() {
        game.humanPassSelected(selectedPassCards.toList())
        game.executePasses()
        passCardsContainer.visibility = View.GONE
        actionButton.visibility = View.GONE
        selectedPassCards.clear()
        updateAllUI()
    }

    private fun setupPlayingPhase() {
        actionButton.visibility = View.VISIBLE
        val currentPlayer = game.getCurrentPlayer()
        if (currentPlayer.isHuman) {
            actionButton.text = "出牌"
            actionButton.isEnabled = false
            val state = game.state
            val leadSuit = state.currentTrick?.leadSuit
            val validCards = game.humanPlayer.validPlays(leadSuit, state.heartsBroken, state.isFirstTrick)
            handView.setCards(game.humanPlayer.hand.toList(), validCards)

            val trickCards = state.currentTrick?.cards ?: emptyList()
            updateTrickDisplay(trickCards)
        } else {
            actionButton.visibility = View.GONE
            updateTrickDisplay(game.state.currentTrick?.cards ?: emptyList())
            handler.postDelayed({ doAITurn() }, 800)
        }

        updateScoreDisplay()
        updatePhaseDisplay()
    }

    private fun playSelectedCard() {
        val card = handView.getSelectedCard()
        if (card == null) {
            Toast.makeText(this, "请先选择一张牌", Toast.LENGTH_SHORT).show()
            return
        }

        val state = game.state
        val leadSuit = state.currentTrick?.leadSuit
        val validCards = game.humanPlayer.validPlays(leadSuit, state.heartsBroken, state.isFirstTrick)

        if (card !in validCards) {
            Toast.makeText(this, "不能出这张牌！", Toast.LENGTH_SHORT).show()
            return
        }

        game.playCard(game.humanPlayer, card)
        handView.clearSelection()
        actionButton.isEnabled = false

        val trickCards = game.state.currentTrick?.cards ?: emptyList()
        updateTrickDisplay(trickCards)

        if (game.state.currentTrick?.isComplete == true) {
            val winner = game.state.lastTrickWinner
            handler.postDelayed({
                showTrickResult(winner)
            }, 600)
        } else if (game.state.phase == GamePhase.PLAYING) {
            handler.postDelayed({ doAITurn() }, 600)
        } else {
            updateAllUI()
        }
    }

    private fun doAITurn() {
        if (game.state.phase != GamePhase.PLAYING) {
            updateAllUI()
            return
        }

        val currentPlayer = game.getCurrentPlayer()
        if (currentPlayer.isHuman) {
            setupPlayingPhase()
            return
        }

        val state = game.state
        if (state.currentTrick == null) {
            setupPlayingPhase()
            return
        }

        val trickCards = state.currentTrick.cards
        val leadSuit = state.currentTrick.leadSuit

        val chosenCard = AIPlayer.chooseCard(
            player = currentPlayer,
            leadSuit = leadSuit,
            heartsBroken = state.heartsBroken,
            isFirstTrick = state.isFirstTrick,
            trickCards = trickCards
        )

        game.playCard(currentPlayer, chosenCard)

        val updatedTrickCards = game.state.currentTrick?.cards ?: emptyList()
        updateTrickDisplay(updatedTrickCards)
        showAIPlayerCard(currentPlayer, chosenCard)

        if (game.state.currentTrick?.isComplete == true) {
            val winner = game.state.lastTrickWinner
            handler.postDelayed({
                showTrickResult(winner)
            }, 800)
        } else {
            handler.postDelayed({ doAITurn() }, 500)
        }
    }

    private fun updateTrickDisplay(trickCards: List<TrickCard>) {
        trickArea.removeAllViews()
        val positions = listOf(Gravity.TOP, Gravity.START, Gravity.BOTTOM, Gravity.END)

        for ((index, tc) in trickCards.withIndex()) {
            val cardView = TextView(this).apply {
                text = formatCard(tc.card)
                textSize = 16f
                setPadding(12.dp, 8.dp, 12.dp, 8.dp)
                setBackgroundColor(0xFFEEEEEE.toInt())
                gravity = Gravity.CENTER

                val playerLabel = if (tc.player.isHuman) "你" else tc.player.name
                text = "$playerLabel\n${formatCard(tc.card)}"
            }
            trickArea.addView(cardView)
        }
    }

    private fun showAIPlayerCard(player: Player, card: Card) {
        playerCardViews[player]?.text = formatCard(card)
        handler.postDelayed({
            playerCardViews[player]?.text = ""
        }, 1500)
    }

    private fun showTrickResult(winner: Player?) {
        if (winner == null) return
        val name = if (winner.isHuman) "你" else winner.name
        Toast.makeText(this, "$name 赢得这一墩", Toast.LENGTH_SHORT).show()

        handler.postDelayed({
            if (game.state.phase == GamePhase.PLAYING) {
                updateAllUI()
            } else if (game.state.phase == GamePhase.ROUND_END || game.state.phase == GamePhase.GAME_OVER) {
                updateAllUI()
            }
        }, 1200)
    }

    private fun showRoundEnd() {
        handView.setCards(emptyList())
        trickArea.removeAllViews()
        actionButton.visibility = View.VISIBLE
        actionButton.text = "下一轮"

        val sb = StringBuilder()
        sb.appendLine("=== 第${game.state.roundNumber}轮结束 ===")
        sb.appendLine()

        val moonShooter = game.allPlayers.find { p ->
            val hasAll = p.takenCards.count { it.isHeart } == 13 && p.takenCards.any { it.isQueenOfSpades }
            val tookNone = p.takenCards.isEmpty()
            hasAll || tookNone
        }

        for (p in game.allPlayers) {
            val name = if (p.isHuman) "你" else p.name
            val roundPoints = if (moonShooter != null && p != moonShooter &&
                moonShooter.takenCards.count { it.isHeart } == 13 && moonShooter.takenCards.any { it.isQueenOfSpades }) {
                26
            } else {
                p.takenCards.sumOf { it.points }
            }
            sb.appendLine("$name: 本轮$roundPoints分 | 总分${p.totalScore}分")
        }

        if (moonShooter != null) {
            val mn = if (moonShooter.isHuman) "你" else moonShooter.name
            sb.appendLine()
            sb.appendLine("$mn 射月成功！其余玩家各+26分")
        }

        scoreText.text = sb.toString()
        updatePhaseDisplay()
    }

    private fun showGameOver() {
        handView.setCards(emptyList())
        trickArea.removeAllViews()
        actionButton.visibility = View.VISIBLE
        actionButton.text = "返回主菜单"

        val winner = game.allPlayers.minByOrNull { it.totalScore }!!
        val winnerName = if (winner.isHuman) "你" else winner.name

        val sb = StringBuilder()
        sb.appendLine("=== 游戏结束 ===")
        sb.appendLine()
        sb.appendLine("最终排名：")
        game.allPlayers.sortedBy { it.totalScore }.forEachIndexed { i, p ->
            val name = if (p.isHuman) "你" else p.name
            sb.appendLine("${i + 1}. $name - ${p.totalScore}分")
        }
        sb.appendLine()
        sb.appendLine("🏆 $winnerName 获胜！(最低分)")
        scoreText.text = sb.toString()
        updatePhaseDisplay()
    }

    private fun formatCard(card: Card): String = "${card.rank.symbol}${card.suit.symbol}"

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
