package com.braintreepayments.api

/**
 * Data class encapsulating the result of a shopper insight api request.
 *
 * This class holds information about the recommended payment methods for a shopper
 * The recommendations include flags for whether payment methods like PayPal or Venmo
 * should be displayed with high priority in the user interface.
 *
 * @property isPayPalRecommended If true, indicates that the PayPal payment option should be given high priority in the checkout UI.
 * @property isVenmoRecommended If true, indicates that the Venmo payment option should be given high priority in the checkout UI.
 */
data class ShopperInsightResult(
    val isPayPalRecommended: Boolean,
    val isVenmoRecommended: Boolean
)
