package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.paypal.messages.config.message.style.PayPalMessageAlignment

/**
 * Text alignment option for a PayPal Message
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */

@ExperimentalBetaApi
enum class PayPalMessagingTextAlignment {

    /**
     * Text aligned to the left
     */
    LEFT,

    /**
     * Text aligned to the center
     */
    CENTER,

    /**
     * Text aligned to the right
     */
    RIGHT;

    internal val internalValue: PayPalMessageAlignment
        get() = when (this) {
            LEFT -> PayPalMessageAlignment.LEFT
            CENTER -> PayPalMessageAlignment.CENTER
            RIGHT -> PayPalMessageAlignment.RIGHT
        }
}
