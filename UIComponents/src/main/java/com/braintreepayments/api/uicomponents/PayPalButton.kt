package com.braintreepayments.api.uicomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat

/**
    * A customizable PayPal branded button to initiate the PayPal flow
 */
class PayPalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {


    private var currentStyle: PayPalButtonStyle = PayPalButtonStyle.BLUE
    private val gradientDrawable = GradientDrawable()
    private var logo: Drawable? = null

    private val logoOffset = resources.getDimension(R.dimen.pp_logo_offset).toInt()
    private val desiredWidth = resources.getDimension(R.dimen.pay_button_width).toInt()
    private val desiredHeight = resources.getDimension(R.dimen.pay_button_height).toInt()
    private val minDesiredWidth = resources.getDimension(R.dimen.pay_button_min_width).toInt()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PayPalButton, 0, 0).apply {
            try {
                currentStyle = PayPalButtonStyle.fromString(getString(R.styleable.PayPalButton_buttonColor))
            } finally {
                recycle()
            }
        }
        setupBackground()
        applyStyle()
    }

    private fun setupBackground() {
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = resources.getDimension(R.dimen.pay_button_corner_radius)
        background = gradientDrawable
        minWidth = minDesiredWidth
    }

    private fun applyStyle() {
        gradientDrawable.setColor(currentStyle.fill)
        val strokeWidth = resources.getDimension(R.dimen.pay_button_border).toInt()
        gradientDrawable.setStroke(strokeWidth, currentStyle.border)
        logo = ContextCompat.getDrawable(context, currentStyle.logoId)
        invalidate()
    }

    /**
     * Sets the color style of the PayPal button.
     *
     * @property color A string representing the button color. Valid values are "white", "black", and "blue".
     */
    fun setButtonColor(color: String) {
        val style = PayPalButtonStyle.fromString(color)
        if (style == currentStyle) return
        currentStyle = style
        applyStyle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = logo ?: return
        val w = d.intrinsicWidth
        val h = d.intrinsicHeight
        val left = (width - w) / 2
        val top = (height - h) / 2 + logoOffset
        d.setBounds(left, top, left + w, top + h)
        d.draw(canvas)
    }
}
