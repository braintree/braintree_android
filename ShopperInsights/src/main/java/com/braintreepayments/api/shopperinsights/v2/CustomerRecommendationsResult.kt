package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Customer recommendations for what payment options to show.
 *
 * @property sessionId The session ID for the customer session.
 * @property isInPayPalNetwork Whether the customer is in the PayPal network.
 * @property paymentRecommendations The payment recommendations for the shopper.
 *
 * Warning: This feature is in beta. It's public API may change or be removed in future releases.
 */
@ExperimentalBetaApi
data class CustomerRecommendationsResult(
    val sessionId: String?,
    val isInPayPalNetwork: Boolean?,
    val paymentRecommendations: List<PaymentOptions>?
)
