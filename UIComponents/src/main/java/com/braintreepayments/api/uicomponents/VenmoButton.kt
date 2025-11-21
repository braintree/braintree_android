package com.braintreepayments.api.uicomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt

/**
 * A customizable Venmo branded button to initiate the Venmo flow
 */
class VenmoButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var logo: Drawable?
    private var colorValue: String
    private var borderColorValue: String

    private val desiredWidth = resources.getDimension(R.dimen.pay_button_width).toInt()
    private val desiredHeight = resources.getDimension(R.dimen.pay_button_height).toInt()
    private val minDesiredWidth = resources.getDimension(R.dimen.pay_button_min_width).toInt()

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.VenmoButton,
            0, 0
        ).apply {
            try {
                val colorAttr = getString(R.styleable.VenmoButton_buttonColor)
                when (colorAttr) {
                    "white" -> {
                        colorValue = "#FFFFFF"
                        borderColorValue = "#555555"
                        logo = ContextCompat.getDrawable(context, R.drawable.venmo_logo_blue)
                    }
                    "black" -> {
                        colorValue = "#000000"
                        borderColorValue = colorValue
                        logo = ContextCompat.getDrawable(context, R.drawable.venmo_logo_white)
                    }
                    else -> {
                        colorValue = "#008CFF"
                        borderColorValue = colorValue
                        logo = ContextCompat.getDrawable(context, R.drawable.venmo_logo_white)
                    }
                }
            } finally {
                recycle()
            }
        }

        val cornerRadiusPx = resources.getDimension(R.dimen.pay_button_corner_radius)
        val strokeWidthPx = resources.getDimension(R.dimen.pay_button_border).toInt()

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
            val top = (height - h) / 2
            d.setBounds(left, top, left + w, top + h)
            d.draw(canvas)
        }
    }
}
