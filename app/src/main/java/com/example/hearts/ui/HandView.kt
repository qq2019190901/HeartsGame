package com.example.hearts.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.example.hearts.model.Card

class HandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        )
        gravity = Gravity.CENTER_VERTICAL
    }

    var onCardSelected: ((Card) -> Unit)? = null
    var onCardDeselected: ((Card) -> Unit)? = null

    private val cardViews = mutableListOf<CardView>()
    private var selectedCard: Card? = null
    private var playableCards: Set<Card> = emptySet()

    init {
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        addView(container)
    }

    fun setCards(cards: List<Card>, validPlays: List<Card> = emptyList()) {
        playableCards = validPlays.toSet()
        selectedCard = null
        container.removeAllViews()
        cardViews.clear()

        for (card in cards) {
            val cardView = CardView(context).apply {
                this.card = card
                isSelected = false
                isPlayable = playableCards.isEmpty() || card in playableCards
                val spacing = 8.dp
                val params = LinearLayout.LayoutParams(70.dp, 105.dp).apply {
                    setMargins(spacing / 2, 0, spacing / 2, 0)
                }
                layoutParams = params
                setOnClickListener {
                    if (!isPlayable) return@setOnClickListener
                    if (this.card == selectedCard) {
                        selectedCard = null
                        isSelected = false
                        onCardDeselected?.invoke(card)
                    } else {
                        cardViews.forEach { it.isSelected = false }
                        selectedCard = this.card
                        isSelected = true
                        onCardSelected?.invoke(card)
                    }
                }
            }
            cardViews.add(cardView)
            container.addView(cardView)
        }
    }

    fun clearSelection() {
        selectedCard = null
        cardViews.forEach { it.isSelected = false }
    }

    fun setCardsPlayable(cards: List<Card>) {
        playableCards = cards.toSet()
        cardViews.forEach { cv ->
            val c = cv.card
            cv.isPlayable = c != null && (playableCards.isEmpty() || c in playableCards)
        }
    }

    fun getSelectedCard(): Card? = selectedCard

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
