package com.braintreepayments.api.uicomponents

/**
 * A representation of the different colors of Venmo buttons
 */
sealed class VenmoButtonColor(val key: Int) {
    internal abstract val default: ButtonColors
    internal abstract val hover: ButtonColors
    internal abstract val focus: ButtonColors
    internal abstract val focusHover: ButtonColors
    internal abstract val pressed: ButtonColors
    internal abstract val logoId: Int
    internal abstract val spinnerId: Int

    object Blue : VenmoButtonColor(key = 0) {
        override val logoId: Int get() = R.drawable.venmo_logo_white
        override val spinnerId: Int get() = R.drawable.avd_spinner_white
        override val default = ButtonColors(R.color.venmo_featured_main, R.color.venmo_featured_main)
        override val hover = ButtonColors(R.color.venmo_featured_hover, R.color.venmo_featured_hover)
        override val focus =
            ButtonColors(R.color.venmo_featured_main, R.color.venmo_featured_main, R.color.venmo_focus_indicator)
        override val focusHover =
            ButtonColors(R.color.venmo_featured_hover, R.color.venmo_featured_hover, R.color.venmo_focus_indicator)
        override val pressed = ButtonColors(R.color.venmo_featured_main_active, R.color.venmo_featured_main_active)
    }

    object Black : VenmoButtonColor(key = 1) {
        override val logoId: Int get() = R.drawable.venmo_logo_white
        override val spinnerId: Int get() = R.drawable.avd_spinner_white
        override val default = ButtonColors(R.color.venmo_tertiary_main, R.color.venmo_tertiary_main)
        override val hover = ButtonColors(R.color.venmo_tertiary_hover, R.color.venmo_tertiary_hover)
        override val focus =
            ButtonColors(R.color.venmo_tertiary_main, R.color.venmo_tertiary_main, R.color.venmo_focus_indicator)
        override val focusHover =
            ButtonColors(R.color.venmo_tertiary_hover, R.color.venmo_tertiary_hover, R.color.venmo_focus_indicator)
        override val pressed = ButtonColors(R.color.venmo_tertiary_main_active, R.color.venmo_tertiary_main_active)
    }

    object White : VenmoButtonColor(key = 2) {
        override val logoId: Int get() = R.drawable.venmo_logo_blue
        override val spinnerId: Int get() = R.drawable.avd_spinner_blue
        override val default = ButtonColors(R.color.venmo_white_main, R.color.venmo_white_border)
        override val hover = ButtonColors(R.color.venmo_white_hover, R.color.venmo_white_border)
        override val focus =
            ButtonColors(R.color.venmo_white_main, R.color.venmo_white_border, R.color.venmo_focus_indicator)
        override val focusHover =
            ButtonColors(R.color.venmo_white_hover, R.color.venmo_white_border, R.color.venmo_focus_indicator)
        override val pressed = ButtonColors(R.color.venmo_white_main_active, R.color.venmo_white_border)
    }

    companion object {
        internal fun fromId(id: Int): VenmoButtonColor = when (id) {
            0 -> Blue
            1 -> Black
            2 -> White
            else -> Blue
        }
    }

    internal data class ButtonColors(
        val fill: Int,
        val border: Int,
        val focusIndicator: Int = R.color.paypal_color_transparent
    )
}
