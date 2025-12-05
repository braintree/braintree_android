package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.MaskFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
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

    private var currentStyle: PayPalButtonColor = PayPalButtonColor.BLUE
    private val gradientDrawable = GradientDrawable()
    private var logo: Drawable? = null

    private val logoOffset = resources.getDimension(R.dimen.pp_logo_offset).toInt()
    private val desiredWidth = resources.getDimension(R.dimen.pay_button_width).toInt()
    private val desiredHeight = resources.getDimension(R.dimen.pay_button_height).toInt()
    private val minDesiredWidth = resources.getDimension(R.dimen.pay_button_min_width).toInt()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PayPalButton, 0, 0).apply {
            try {
                currentStyle = PayPalButtonColor.fromId(getInt(R.styleable.PayPalButton_paymentButtonColor, 0))
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

    private fun rippleEffect() {
        val rippleEffectWhite = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.black)),
            null,
            ShapeDrawable(RectShape()).apply { paint.color = Color.WHITE })
        val rippleEffectBlack = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white)),
            null,
            ShapeDrawable(RectShape()).apply { paint.color = Color.BLACK })
        val rippleEffectBlue = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white)),
            null,
            ShapeDrawable(RectShape()).apply { paint.color = PayPalButtonColor.BLUE.fill })
        val rippleDrawable = when (currentStyle) {
            PayPalButtonColor.BLUE -> rippleEffectBlue
            PayPalButtonColor.WHITE -> rippleEffectWhite
            PayPalButtonColor.BLACK -> rippleEffectBlack
        }
        isClickable = true
        isFocusable = true

        foreground = rippleDrawable
    }

    private fun applyStyle() {
//        updateButtonAppearance()
        rippleEffect()
        logo = ContextCompat.getDrawable(context, currentStyle.logoId)
        invalidate()
    }

    private fun updateButtonAppearance() {
        val strokeWidth = resources.getDimension(R.dimen.pay_button_border).toInt()

        when {
            !isEnabled -> {
                // Disabled state: reduced opacity
                gradientDrawable.setColor(applyAlpha(currentStyle.fill, 0.4f))
                gradientDrawable.setStroke(strokeWidth, applyAlpha(currentStyle.border, 0.4f))
                alpha = 0.5f
            }
            isPressed -> {
                // Pressed state: darker fill
                gradientDrawable.setColor(darkenColor(currentStyle.fill, 0.8f))
                gradientDrawable.setStroke(strokeWidth, currentStyle.border)
                alpha = 1.0f
            }
            isHovered || isFocused -> {
                // Hover/Focus state: slightly lighter fill
                gradientDrawable.setColor(lightenColor(currentStyle.fill, 1.1f))
                gradientDrawable.setStroke(strokeWidth, currentStyle.border)
                alpha = 1.0f
            }
            else -> {
                // Default state
                gradientDrawable.setColor(currentStyle.fill)
                gradientDrawable.setStroke(strokeWidth, currentStyle.border)
                alpha = 1.0f
            }
        }
    }

    private fun applyAlpha(color: Int, alphaFactor: Float): Int {
        val alpha = ((color shr 24 and 0xFF) * alphaFactor).toInt()
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) * factor).toInt()
        val g = ((color shr 8 and 0xFF) * factor).toInt()
        val b = ((color and 0xFF) * factor).toInt()
        val a = (color shr 24 and 0xFF)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = minOf(255, ((color shr 16 and 0xFF) * factor).toInt())
        val g = minOf(255, ((color shr 8 and 0xFF) * factor).toInt())
        val b = minOf(255, ((color and 0xFF) * factor).toInt())
        val a = (color shr 24 and 0xFF)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateButtonAppearance()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        updateButtonAppearance()
    }

    /**
     * Sets the color of the PayPal button
     *
     * @property color Value representing the button color. Valid values are BLUE, BLACK, and WHITE
     */
    fun setButtonColor(color: PayPalButtonColor) {
        val style = color
        if (style == currentStyle) return
        currentStyle = style
        applyStyle()
    }
}
