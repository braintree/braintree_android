package com.braintreepayments.api.shopperinsights

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Representation of a user phone number.
 * @property countryCode The international country code for the shopper's phone number
 * (e.g., "1" for the United States).
 * @property nationalNumber The national segment of the shopper's phone number
 * (excluding the country code).
 */
@ExperimentalBetaApi
data class ShopperInsightsBuyerPhone(
    var countryCode: String,
    var nationalNumber: String
)
