package com.braintreepayments.api

/**
 * Representation of a user phone number.
 * @property countryCode The international country code for the shopper's phone number
 * (e.g., "1" for the United States).
 * @property nationalNumber The national segment of the shopper's phone number
 * (excluding the country code).
 */

data class BuyerPhone(
    var countryCode: String,
    var nationalNumber: String
)

/**
 * Data class representing a request for shopper insights.
 */
sealed class ShopperInsightRequest {
    data class Email(var email: String) : ShopperInsightRequest()
    data class Phone(
        var phone: BuyerPhone
    ) : ShopperInsightRequest()
    data class EmailAndPhone(
        var email: String,
        var phone: BuyerPhone
    ) : ShopperInsightRequest()
}
