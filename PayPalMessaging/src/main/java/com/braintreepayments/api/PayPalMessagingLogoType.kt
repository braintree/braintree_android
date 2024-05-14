package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageLogoType

/**
 * Logo type option for a PayPal Message
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
enum class PayPalMessagingLogoType {
    /**
     * Primary logo including both the PayPal monogram and logo
     */
    PRIMARY,

    /**
     * Alternative logo including just the PayPal monogram
     */
    ALTERNATIVE,

    /**
     * PayPal logo positioned inline within the message
     */
    INLINE,

    /**
     * "PayPal" as bold text inline with the message
     */
    NONE;

    internal val internalValue: PayPalMessageLogoType
        get() = when (this) {
            PRIMARY -> PayPalMessageLogoType.PRIMARY
            ALTERNATIVE -> PayPalMessageLogoType.ALTERNATIVE
            INLINE -> PayPalMessageLogoType.INLINE
            NONE -> PayPalMessageLogoType.NONE
        }
}
