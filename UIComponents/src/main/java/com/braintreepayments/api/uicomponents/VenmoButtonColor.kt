package com.braintreepayments.api.uicomponents

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt

/**
 * A representation of the different colors of Venmo buttons
 */
sealed class VenmoButtonColor(val key: Int) {
    abstract val default: ButtonColors
    abstract val hover: ButtonColors
    abstract val focus: ButtonColors
    abstract val focusHover: ButtonColors
    abstract val pressed: ButtonColors
    abstract val logoId: Int
    abstract val spinnerId: Int

    object Blue : VenmoButtonColor(key = 0) {
        override val logoId: Int get() = R.drawable.venmo_logo_white
        override val spinnerId: Int get() = R.drawable.avd_spinner_white
        override val default = ButtonColors(VENMO_FEATURED_MAIN, VENMO_FEATURED_MAIN)
        override val hover = ButtonColors(VENMO_FEATURED_HOVER, VENMO_FEATURED_HOVER)
        override val focus = ButtonColors(VENMO_FEATURED_MAIN, VENMO_FEATURED_MAIN, VENMO_FOCUS_INDICATOR)
        override val focusHover = ButtonColors(VENMO_FEATURED_HOVER, VENMO_FEATURED_HOVER, VENMO_FOCUS_INDICATOR)
        override val pressed = ButtonColors(VENMO_FEATURED_MAIN_ACTIVE, VENMO_FEATURED_MAIN_ACTIVE)
    }

    object Black : VenmoButtonColor(key = 1) {
        override val logoId: Int get() = R.drawable.venmo_logo_white
        override val spinnerId: Int get() = R.drawable.avd_spinner_white
        override val default = ButtonColors(VENMO_TERTIARY_MAIN, VENMO_TERTIARY_MAIN)
        override val hover = ButtonColors(VENMO_TERTIARY_HOVER, VENMO_TERTIARY_HOVER)
        override val focus = ButtonColors(VENMO_TERTIARY_MAIN, VENMO_TERTIARY_MAIN, VENMO_FOCUS_INDICATOR)
        override val focusHover = ButtonColors(VENMO_TERTIARY_HOVER, VENMO_TERTIARY_HOVER, VENMO_FOCUS_INDICATOR)
        override val pressed = ButtonColors(VENMO_TERTIARY_MAIN_ACTIVE, VENMO_TERTIARY_MAIN_ACTIVE)
    }

    object White : VenmoButtonColor(key = 2) {
        override val logoId: Int get() = R.drawable.venmo_logo_blue
        override val spinnerId: Int get() = R.drawable.avd_spinner_black
        override val default = ButtonColors(VENMO_WHITE_MAIN, VENMO_WHITE_BORDER)
        override val hover = ButtonColors(VENMO_WHITE_HOVER, VENMO_WHITE_BORDER)
        override val focus = ButtonColors(VENMO_WHITE_MAIN, VENMO_WHITE_BORDER, VENMO_FOCUS_INDICATOR)
        override val focusHover = ButtonColors(VENMO_WHITE_HOVER, VENMO_WHITE_BORDER, VENMO_FOCUS_INDICATOR)
        override val pressed = ButtonColors(VENMO_WHITE_MAIN_ACTIVE, VENMO_WHITE_BORDER)
    }

    companion object {
        internal fun fromId(id: Int): VenmoButtonColor = when (id) {
            0 -> Blue
            1 -> Black
            2 -> White
            else -> Blue
        }

        private val VENMO_FEATURED_MAIN = "#008CFF".toColorInt()
        private val VENMO_FEATURED_HOVER = "#0073E0".toColorInt()
        private val VENMO_FEATURED_MAIN_ACTIVE = "#0074FF".toColorInt()
        private val VENMO_FOCUS_INDICATOR = "#0066F5".toColorInt()
        private val VENMO_TERTIARY_MAIN = "#000000".toColorInt()
        private val VENMO_TERTIARY_HOVER = "#333333".toColorInt()
        private val VENMO_TERTIARY_MAIN_ACTIVE = "#696969".toColorInt()
        private val VENMO_WHITE_BORDER = "#555555".toColorInt()
        private val VENMO_WHITE_MAIN = "#FFFFFF".toColorInt()
        private val VENMO_WHITE_HOVER = "#F2F2F2".toColorInt()
        private val VENMO_WHITE_MAIN_ACTIVE = "#E9E9E9".toColorInt()
    }

    data class ButtonColors(
        @ColorInt val fill: Int,
        @ColorInt val border: Int,
        @ColorInt val focusIndicator: Int = Color.argb(0, 0, 0, 0)
    )
}
