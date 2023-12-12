package com.braintreepayments.api

/**
 * Data class representing a request for shopper insights.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
sealed class ShopperInsightsRequest {
    data class Email(var email: String) : ShopperInsightsRequest()
    data class Phone(
        var phone: BuyerPhone
    ) : ShopperInsightsRequest()
    data class EmailAndPhone(
        var email: String,
        var phone: BuyerPhone
    ) : ShopperInsightsRequest()
}
