package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageAlign

/**
 * Text alignment option for a PayPal Message
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
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

    internal val textAlignmentRawValue: PayPalMessageAlign
        get() = when(this) {
            LEFT -> PayPalMessageAlign.LEFT
            CENTER -> PayPalMessageAlign.CENTER
            RIGHT -> PayPalMessageAlign.RIGHT
        }
}