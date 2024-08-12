package com.braintreepayments.api.shopperinsights

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Data class representing a request for shopper insights.
 *
 * @property email The shopper's email address
 * @property phone The shopper's phone number
 *
 * One of [email] or [phone] must be provided to get shopper insights.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
@ExperimentalBetaApi
data class ShopperInsightsRequest(
    var email: String?,
    var phone: ShopperInsightsBuyerPhone?
)
