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
        override val default = ButtonColors(PAYPAL_FEATURED_MAIN, PAYPAL_FEATURED_MAIN)
        override val hover = ButtonColors(PAYPAL_FEATURED_HOVER, PAYPAL_FEATURED_HOVER)
        override val focus = ButtonColors(PAYPAL_FEATURED_MAIN, PAYPAL_FEATURED_MAIN, PAYPAL_FOCUS_INDICATOR)
        override val focusHover = ButtonColors(PAYPAL_FEATURED_HOVER, PAYPAL_FEATURED_HOVER, PAYPAL_FOCUS_INDICATOR)
        override val pressed = ButtonColors(PAYPAL_FEATURED_MAIN_ACTIVE, PAYPAL_FEATURED_MAIN_ACTIVE)
    }

    object Black : PayPalButtonColor(key = 1) {
        override val logoId: Int get() = R.drawable.paypal_logo_white
        override val spinnerId: Int get() = R.drawable.avd_spinner_white
        override val default = ButtonColors(PAYPAL_TERTIARY_MAIN, PAYPAL_TERTIARY_MAIN)
        override val hover = ButtonColors(PAYPAL_TERTIARY_HOVER, PAYPAL_TERTIARY_HOVER)
        override val focus = ButtonColors(PAYPAL_TERTIARY_MAIN, PAYPAL_TERTIARY_MAIN, PAYPAL_FOCUS_INDICATOR)
        override val focusHover = ButtonColors(PAYPAL_TERTIARY_HOVER, PAYPAL_TERTIARY_HOVER, PAYPAL_FOCUS_INDICATOR)
        override val pressed = ButtonColors(PAYPAL_TERTIARY_MAIN_ACTIVE, PAYPAL_TERTIARY_MAIN_ACTIVE)
    }

    object White : PayPalButtonColor(key = 2) {
        override val logoId: Int get() = R.drawable.paypal_logo_black
        override val spinnerId: Int get() = R.drawable.avd_spinner_black
        override val default = ButtonColors(PAYPAL_WHITE_MAIN, PAYPAL_WHITE_BORDER)
        override val hover = ButtonColors(PAYPAL_WHITE_HOVER, PAYPAL_WHITE_BORDER)
        override val focus = ButtonColors(PAYPAL_WHITE_MAIN, PAYPAL_WHITE_BORDER, PAYPAL_FOCUS_INDICATOR)
        override val focusHover = ButtonColors(PAYPAL_WHITE_HOVER, PAYPAL_WHITE_BORDER, PAYPAL_FOCUS_INDICATOR)
        override val pressed = ButtonColors(PAYPAL_WHITE_MAIN_ACTIVE, PAYPAL_WHITE_BORDER)
    }

    internal data class ButtonColors(
        @ColorInt val fill: Int,
        @ColorInt val border: Int,
        @ColorInt val focusIndicator: Int = Color.argb(0, 0, 0, 0)
    )

    companion object {
        internal fun fromId(id: Int): PayPalButtonColor = when (id) {
            0 -> Blue
            1 -> Black
            2 -> White
            else -> Blue
        }

        private val PAYPAL_FEATURED_MAIN = "#60CDFF".toColorInt()
        private val PAYPAL_FEATURED_HOVER = "#54B4E0".toColorInt()
        private val PAYPAL_FEATURED_MAIN_ACTIVE = "#3DB5FF".toColorInt()
        private val PAYPAL_FOCUS_INDICATOR = "#0066F5".toColorInt()
        private val PAYPAL_TERTIARY_MAIN = "#000000".toColorInt()
        private val PAYPAL_TERTIARY_HOVER = "#333333".toColorInt()
        private val PAYPAL_TERTIARY_MAIN_ACTIVE = "#696969".toColorInt()
        private val PAYPAL_WHITE_MAIN = "#FFFFFF".toColorInt()
        private val PAYPAL_WHITE_HOVER = "#F2F2F2".toColorInt()
        private val PAYPAL_WHITE_MAIN_ACTIVE = "#E9E9E9".toColorInt()
        private val PAYPAL_WHITE_BORDER = "#555555".toColorInt()
    }
}
