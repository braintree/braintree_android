package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessagePageType

/**
 * Message location within an application
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
enum class PayPalMessagingPageType {

    /**
     * Home view
     */
    HOME,

    /**
     * Individual product details view
     */
    PRODUCT_DETAILS,

    /**
     * Product listing
     */
    PRODUCT_LISTING,

    /**
     * Shopping cart view
     */
    CART,

    /**
     * Popover shopping cart view that covers part of the view
     */
    MINI_CART,

    /**
     * Checkout view
     */
    CHECKOUT,

    /**
     * Search results
     */
    SEARCH_RESULTS;

    internal val internalValue: PayPalMessagePageType
        get() = when (this) {
            HOME -> PayPalMessagePageType.HOME
            PRODUCT_DETAILS -> PayPalMessagePageType.PRODUCT_DETAILS
            PRODUCT_LISTING -> PayPalMessagePageType.PRODUCT_LISTING
            CART -> PayPalMessagePageType.CART
            MINI_CART -> PayPalMessagePageType.MINI_CART
            CHECKOUT -> PayPalMessagePageType.CHECKOUT
            SEARCH_RESULTS -> PayPalMessagePageType.SEARCH_RESULTS
        }
}
