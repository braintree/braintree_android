package com.braintreepayments.api.uicomponents

/**
 * A representation of the different colors of PayPal Pay Later buttons
 */
sealed class PayLaterButtonColor(
    val key: Int,
    private val payPalButtonColor: PayPalButtonColor
) {
    internal abstract val logoId: Int

    // Pay Later shares fill, border, focus and spinner styling with PayPal; only the wordmark differs.
    internal fun toStyle(): PaymentButtonStyle =
        payPalButtonColor.toStyle().copy(logoId = logoId)

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
