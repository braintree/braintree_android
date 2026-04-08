package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * PayPal campaign details; aligns with iOS `BTPayPalCampaign`. Each entry serializes as `{ "id": "<campaign-id>" }`.
 *
 * Included under the `paypal_campaigns` key in Shopper Insights v2 request variables (create/update session,
 * generate recommendations).
 *
 * Warning: This feature is in beta. It's public API may change or be removed in future releases.
 */
@ExperimentalBetaApi
data class PayPalCampaign(
    val id: String,
)
