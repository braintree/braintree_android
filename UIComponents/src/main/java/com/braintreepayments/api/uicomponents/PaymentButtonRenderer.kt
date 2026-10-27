package com.braintreepayments.api.uicomponents

import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Owns the drawing, sizing and loading-state behavior shared by the branded payment buttons.
 *
 * A button creates one renderer for itself, then forwards its view callbacks ([onMeasure], [onDraw],
 * [updateAppearance]) to it. Buttons only supply a [PaymentButtonStyle]; they never touch the drawables.
 */
internal class PaymentButtonRenderer(
    private val view: View,
    initialStyle: PaymentButtonStyle
) {

    private val context = view.context
    private val resources = view.resources

    private val gradientDrawable = GradientDrawable()
    private val focusIndicatorDrawable = GradientDrawable()
    private var logo: Drawable? = null

    private val defaultLogoOffset = resources.getDimension(R.dimen.pp_logo_offset).toInt()
    private var logoOffset = defaultLogoOffset
    private val desiredWidth = resources.getDimension(R.dimen.pay_button_width).toInt()
    private val desiredHeight = resources.getDimension(R.dimen.pay_button_height).toInt()

    var style: PaymentButtonStyle = initialStyle
        private set

    /**
     * Installs the layered background on the view and applies the initial style and minimum size.
     * Call once from the button's `init` block.
     */
    fun setup() {
        val cornerRadius = resources.getDimension(R.dimen.pay_button_corner_radius)
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = cornerRadius
        focusIndicatorDrawable.shape = GradientDrawable.RECTANGLE
        focusIndicatorDrawable.cornerRadius = cornerRadius

        val layerDrawable = LayerDrawable(arrayOf(focusIndicatorDrawable, gradientDrawable))
        val focusPadding = resources.getDimension(R.dimen.pay_button_focus_padding).toInt()
        layerDrawable.setLayerInset(1, focusPadding, focusPadding, focusPadding, focusPadding)
        view.background = layerDrawable

        applyStyle()
        setMinimumSize()
    }

    /** Switches to [newStyle] and redraws. No-op if the style is unchanged. */
    fun setStyle(newStyle: PaymentButtonStyle) {
        if (newStyle == style) return
        style = newStyle
        applyStyle()
    }

    /** Swaps the logo for the spinner and shifts it to the center. Call when the flow launches. */
    fun showLoading() {
        view.isEnabled = false
        logo = ContextCompat.getDrawable(context, style.spinnerId)
        (logo as? Animatable)?.start()
        logoOffset = 0
        view.invalidate()
    }

    /** Re-enables the view and restores the logo. Call when the flow ends, successfully or not. */
    fun hideLoading() {
        view.isEnabled = true
        (logo as? Animatable)?.stop()
        logo = ContextCompat.getDrawable(context, style.logoId)
        logoOffset = defaultLogoOffset
        view.invalidate()
    }

    /** Recolors the fill, border and focus indicator for the view's current pressed/hovered/focused state. */
    fun updateAppearance() {
        val strokeWidth = resources.getDimension(R.dimen.pay_button_border).toInt()
        val focusStrokeWidth = resources.getDimension(R.dimen.pay_button_focus_border).toInt()

        val (colors, indicatorWidth) = when {
            view.isPressed -> style.pressed to strokeWidth
            view.isHovered -> style.hover to focusStrokeWidth
            view.isFocused -> style.focus to focusStrokeWidth
            else -> style.default to focusStrokeWidth
        }

        gradientDrawable.setColor(ContextCompat.getColor(context, colors.fill))
        gradientDrawable.setStroke(strokeWidth, ContextCompat.getColor(context, colors.border))
        focusIndicatorDrawable.setStroke(indicatorWidth, ContextCompat.getColor(context, colors.focusIndicator))
    }

    /** Returns the measured (width, height) to pass to `setMeasuredDimension`. */
    fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Pair<Int, Int> {
        val width = View.resolveSize(desiredWidth, widthMeasureSpec).coerceAtLeast(view.minimumWidth)
        val height = View.resolveSize(desiredHeight, heightMeasureSpec).coerceAtLeast(view.minimumHeight)
        return width to height
    }

    /** Draws the centered logo (or spinner). Call after `super.onDraw`. */
    fun onDraw(canvas: Canvas) {
        logo?.let { logoDrawable ->
            val w = logoDrawable.intrinsicWidth
            val h = logoDrawable.intrinsicHeight
            val left = (view.width - w) / 2
            val top = (view.height - h) / 2 + logoOffset
            logoDrawable.setBounds(left, top, left + w, top + h)
            logoDrawable.draw(canvas)
        }
    }

    private fun applyStyle() {
        updateAppearance()
        logo = ContextCompat.getDrawable(context, style.logoId)
        view.invalidate()
    }

    private fun setMinimumSize() {
        view.minimumHeight = desiredHeight
        val logoDrawable = logo ?: return
        val fullButtonFocusPadding = resources.getDimension(R.dimen.pay_button_focus_padding).toInt() * 2
        val logoBufferPadding = fullButtonFocusPadding
        view.minimumWidth = logoDrawable.intrinsicWidth + fullButtonFocusPadding + logoBufferPadding
    }
}
