package com.braintreepayments.api

/**
 * Data class representing a request for shopper insights.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
data class ShopperInsightsRequest(
    var email: String?,
    var phone: BuyerPhone?
)
