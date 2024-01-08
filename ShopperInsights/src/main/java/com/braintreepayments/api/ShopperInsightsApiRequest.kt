package com.braintreepayments.api

/**
 * Data class representing a request for shopper insights api.
 *
 * @property request The request given to us by the merchant
 * @property merchantId The merchant's ID
 * @property currencyCode The currency code
 * @property countryCode The country code
 *
 * [merchantId] [currencyCode] [countryCode] are needed for Venmo recommended results
 *
 */
data class ShopperInsightsApiRequest(
    var request: ShopperInsightsRequest,
    var merchantId: String,
    var currencyCode: String,
    var countryCode: String
)
