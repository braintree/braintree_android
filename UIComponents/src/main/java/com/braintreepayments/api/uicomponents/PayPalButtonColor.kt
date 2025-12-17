package com.braintreepayments.api.uicomponents

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt

/**
 * A representation of the different colors of PayPal buttons
 */
sealed class PayPalButtonColor(val key: Int) {
    internal abstract val default: ButtonColors
    internal abstract val hover: ButtonColors
    internal abstract val focus: ButtonColors
    internal abstract val focusHover: ButtonColors
    internal abstract val pressed: ButtonColors
    internal abstract val logoId: Int
    internal abstract val spinnerId: Int

    object Blue : PayPalButtonColor(key = 0) {
        override val logoId: Int get() = R.drawable.paypal_logo_black
        override val spinnerId: Int get() = R.drawable.avd_spinner_black
        override val default = ButtonColors(R.color.paypal_featured_main, R.color.paypal_featured_main)
        override val hover = ButtonColors(R.color.paypal_featured_hover, R.color.paypal_featured_hover)
        override val focus =
            ButtonColors(R.color.paypal_featured_main, R.color.paypal_featured_main, R.color.paypal_focus_indicator)
        override val focusHover =
            ButtonColors(R.color.paypal_featured_hover, R.color.paypal_featured_hover, R.color.paypal_focus_indicator)
        override val pressed = ButtonColors(R.color.paypal_featured_main_active, R.color.paypal_featured_main_active)
    }

    object Black : PayPalButtonColor(key = 1) {
        override val logoId: Int get() = R.drawable.paypal_logo_white
        override val spinnerId: Int get() = R.drawable.avd_spinner_white
        override val default = ButtonColors(R.color.paypal_tertiary_main, R.color.paypal_tertiary_main)
        override val hover = ButtonColors(R.color.paypal_tertiary_hover, R.color.paypal_tertiary_hover)
        override val focus =
            ButtonColors(R.color.paypal_tertiary_main, R.color.paypal_tertiary_main, R.color.paypal_focus_indicator)
        override val focusHover =
            ButtonColors(R.color.paypal_tertiary_hover, R.color.paypal_tertiary_hover, R.color.paypal_focus_indicator)
        override val pressed = ButtonColors(R.color.paypal_tertiary_main_active, R.color.paypal_tertiary_main_active)
    }

    object White : PayPalButtonColor(key = 2) {
        override val logoId: Int get() = R.drawable.paypal_logo_black
        override val spinnerId: Int get() = R.drawable.avd_spinner_black
        override val default = ButtonColors(R.color.paypal_white_main, R.color.paypal_white_border)
        override val hover = ButtonColors(R.color.paypal_white_hover, R.color.paypal_white_border)
        override val focus =
            ButtonColors(R.color.paypal_white_main, R.color.paypal_white_border, R.color.paypal_focus_indicator)
        override val focusHover =
            ButtonColors(R.color.paypal_white_hover, R.color.paypal_white_border, R.color.paypal_focus_indicator)
        override val pressed = ButtonColors(R.color.paypal_white_main_active, R.color.paypal_white_border)
    }

    internal data class ButtonColors(
        val fill: Int,
        val border: Int,
        val focusIndicator: Int = R.color.color_transparent
    )

    companion object {
        internal fun fromId(id: Int): PayPalButtonColor = when (id) {
            0 -> Blue
            1 -> Black
            2 -> White
            else -> Blue
        }
    }
}
