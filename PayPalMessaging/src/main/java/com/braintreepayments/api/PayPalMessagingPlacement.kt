package com.braintreepayments.api

/**
 * Message location within an application
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
enum class PayPalMessagingPlacement {

    /**
     * Home view
     */
    HOME,

    /**
     * Category view displaying multiple products
     */
    CATEGORY,

    /**
     * Individual product view
     */
    PRODUCT,

    /**
     * Shopping cart view
     */
    CART,

    /**
     * Checkout view
     */
    PAYMENT;

    internal val placementRawValue: String
        get() = when(this) {
            HOME -> "HOME"
            CATEGORY -> "CATEGORY"
            PRODUCT -> "PRODUCT"
            CART -> "CART"
            PAYMENT -> "PAYMENT"
        }
}
