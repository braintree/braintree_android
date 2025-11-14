package com.braintreepayments.api.uicomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import kotlin.text.toInt
import kotlin.times

class PayPalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var logo: Drawable?
    private var colorValue: String
    private var borderColorValue: String

    // The logo is supposed to be visually not absolutely centered
    private val logoOffset = (1.5f * resources.displayMetrics.density).toInt()
    private val desiredWidth = (300 * resources.displayMetrics.density).toInt()
    private val desiredHeight = (45 * resources.displayMetrics.density).toInt()
    private val minDesiredWidth = (75 * resources.displayMetrics.density).toInt()

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PayPalButton,
            0, 0
        ).apply {
            try {
                val colorAttr = getString(R.styleable.PayPalButton_buttonColor)
                when (colorAttr) {
                    "white" -> {
                        colorValue = "#FFFFFF"
                        borderColorValue = "#555555"
                        logo = ContextCompat.getDrawable(context, R.drawable.paypal_logo_black)
                    }
                    "black" -> {
                        colorValue = "#000000"
                        borderColorValue = colorValue
                        logo = ContextCompat.getDrawable(context, R.drawable.paypal_logo_white)
                    }
                    else -> {
                        colorValue = "#60CDFF"
                        borderColorValue = colorValue
                        logo = ContextCompat.getDrawable(context, R.drawable.paypal_logo_black)
                    }
                }
            } finally {
                recycle()
            }
        }

        val cornerRadiusPx = 4 * resources.displayMetrics.density
        val strokeWidthPx = (1 * resources.displayMetrics.density).toInt()

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(colorValue.toColorInt())
            setStroke(strokeWidthPx, borderColorValue.toColorInt())
        }
        minWidth = minDesiredWidth
        background = bg
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        logo?.let { d ->
            val w = d.intrinsicWidth
            val h = d.intrinsicHeight
            val left = (width - w) / 2
            val top = (height - h) / 2 + logoOffset
            d.setBounds(left, top, left + w, top + h)
            d.draw(canvas)
        }
    }
}
