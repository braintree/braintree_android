package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageColor

/**
 * Text and logo color option for a PayPal Message
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
enum class PayPalMessagingColor {
    /**
     * Black text with a color logo
     */
    BLACK,

    /**
     * White text with a color logo
     */
    WHITE,

    /**
     * Black text with a black logo
     */
    MONOCHROME,

    /**
     * Black text with a desaturated logo
     */
    GRAYSCALE;

    internal val messageColorRawValue: PayPalMessageColor
        get() = when (this) {
            BLACK -> PayPalMessageColor.BLACK
            WHITE -> PayPalMessageColor.WHITE
            MONOCHROME -> PayPalMessageColor.MONOCHROME
            GRAYSCALE -> PayPalMessageColor.GRAYSCALE
        }
}
