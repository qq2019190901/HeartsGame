package com.example.hearts.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.hearts.model.Card
import com.example.hearts.model.Suit

class CardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var card: Card? = null
        set(value) {
            field = value
            invalidate()
        }

    var isSelected: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var isPlayable: Boolean = true
        set(value) {
            field = value
            alpha = if (value) 1.0f else 0.4f
        }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFF333333.toInt()
    }
    private val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val suitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val rect = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cornerRadius = 10f
        rect.set(0f, 0f, w, h)

        // Card background
        cardPaint.style = Paint.Style.FILL
        cardPaint.color = if (isSelected) 0xFFE3F2FD.toInt() else 0xFFFFFFFF.toInt()
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cardPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val c = card ?: return

        // Colors
        val color = if (c.suit == Suit.HEARTS || c.suit == Suit.DIAMONDS)
            0xFFE53935.toInt() else 0xFF000000.toInt()

        rankPaint.color = color
        suitPaint.color = color
        cornerPaint.color = color

        // Corner text (rank + suit symbol)
        val cornerText = "${c.rank.symbol}${c.suit.symbol}"
        canvas.drawText(cornerText, 8f, 22f, cornerPaint)

        // Center suit symbol
        val suitText = c.suit.symbol
        val suitWidth = suitPaint.measureText(suitText)
        canvas.drawText(suitText, (w - suitWidth) / 2, h / 2 + 12, suitPaint)

        // Rank text below suit
        val rankText = c.rank.symbol
        val rankWidth = rankPaint.measureText(rankText)
        canvas.drawText(rankText, (w - rankWidth) / 2, h / 2 + 44, rankPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredW = 70.dp
        val desiredH = 105.dp
        setMeasuredDimension(
            resolveSize(desiredW, widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec)
        )
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
