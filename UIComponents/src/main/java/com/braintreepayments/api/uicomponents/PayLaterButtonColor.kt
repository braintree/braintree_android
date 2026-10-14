package com.braintreepayments.api.uicomponents

/**
 * A representation of the different colors of PayPal Pay Later buttons
 */
sealed class PayLaterButtonColor(
    val key: Int,
    private val payPalButtonColor: PayPalButtonColor
) {
    internal abstract val logoId: Int

    // PayPal Pay Later buttons share fill, border, focus, and spinner styling with the PayPal buttons.
    internal val default: PayPalButtonColor.ButtonColors get() = payPalButtonColor.default
    internal val hover: PayPalButtonColor.ButtonColors get() = payPalButtonColor.hover
    internal val focus: PayPalButtonColor.ButtonColors get() = payPalButtonColor.focus
    internal val focusHover: PayPalButtonColor.ButtonColors get() = payPalButtonColor.focusHover
    internal val pressed: PayPalButtonColor.ButtonColors get() = payPalButtonColor.pressed
    internal val spinnerId: Int get() = payPalButtonColor.spinnerId

    object Blue : PayLaterButtonColor(key = 0, payPalButtonColor = PayPalButtonColor.Blue) {
        override val logoId: Int get() = R.drawable.pay_later_logo_black
    }

    object Black : PayLaterButtonColor(key = 1, payPalButtonColor = PayPalButtonColor.Black) {
        override val logoId: Int get() = R.drawable.pay_later_logo_white
    }

    object White : PayLaterButtonColor(key = 2, payPalButtonColor = PayPalButtonColor.White) {
        override val logoId: Int get() = R.drawable.pay_later_logo_black
    }

    companion object {
        internal fun fromId(id: Int): PayLaterButtonColor = when (id) {
            0 -> Blue
            1 -> Black
            2 -> White
            else -> Blue
        }
    }
}
