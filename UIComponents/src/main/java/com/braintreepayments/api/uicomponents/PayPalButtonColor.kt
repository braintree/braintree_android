package com.braintreepayments.api.uicomponents

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt

/**
 * A representation of the different colors of PayPal buttons
 */
@Suppress("MagicNumber", "CyclomaticComplexMethod")
sealed class PayPalButtonColor(val key: Int) {
    abstract val default: ButtonColors
    abstract val hover: ButtonColors
    abstract val focus: ButtonColors
    abstract val focusHover: ButtonColors
    abstract val pressed: ButtonColors
    abstract val logoId: Int
    abstract val spinnerId: Int

    object Blue : PayPalButtonColor(key = 0) {
        override val logoId: Int get() = R.drawable.paypal_logo_black
        override val spinnerId: Int get() = R.drawable.avd_spinner_black
        override val default = ButtonColors(PAYPAL_FEATURED_MAIN, PAYPAL_FEATURED_MAIN)
        override val hover = ButtonColors(PAYPAL_FEATURED_HOVER, PAYPAL_FEATURED_HOVER)
        override val focus = ButtonColors(PAYPAL_FEATURED_MAIN, PAYPAL_FEATURED_MAIN, PAYPAL_FOCUS_INDICATOR)
        override val focusHover =
            ButtonColors(PAYPAL_FEATURED_HOVER, PAYPAL_FEATURED_HOVER, PAYPAL_FOCUS_INDICATOR)
        override val pressed = ButtonColors(PAYPAL_FEATURED_MAIN_ACTIVE, PAYPAL_FEATURED_MAIN_ACTIVE)
    }

    object Black : PayPalButtonColor(key = 1) {
        override val logoId: Int get() = R.drawable.paypal_logo_white
        override val spinnerId: Int get() = R.drawable.avd_spinner_white
        override val default = ButtonColors("#000000".toColorInt(), "#000000".toColorInt())
        override val hover = ButtonColors("#333333".toColorInt(), "#333333".toColorInt())
        override val focus = ButtonColors("#000000".toColorInt(), "#000000".toColorInt(), "#0066F5".toColorInt())
        override val focusHover =
            ButtonColors("#333333".toColorInt(), "#333333".toColorInt(), "#0066F5".toColorInt())
        override val pressed = ButtonColors("#696969".toColorInt(), "#696969".toColorInt())
    }

    object White : PayPalButtonColor(key = 2) {
        override val logoId: Int get() = R.drawable.paypal_logo_black
        override val spinnerId: Int get() = R.drawable.avd_spinner_black
        override val default = ButtonColors("#FFFFFF".toColorInt(), "#555555".toColorInt())
        override val hover = ButtonColors("#F2F2F2".toColorInt(), "#555555".toColorInt())
        override val focus = ButtonColors("#FFFFFF".toColorInt(), "#555555".toColorInt(), "#0066F5".toColorInt())
        override val focusHover =
            ButtonColors("#F2F2F2".toColorInt(), "#555555".toColorInt(), "#0066F5".toColorInt())
        override val pressed = ButtonColors("#E9E9E9".toColorInt(), "#555555".toColorInt())
    }

    data class ButtonColors(
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
    }
}
