package com.braintreepayments.api

/**
 * Data class representing a request for shopper insights.
 *
 * @property email The email address of the shopper.
 * @property phoneCountryCode The international country code for the shopper's phone number
 * (e.g., "+1" for the United States).
 * @property phoneNationalNumber The national segment of the shopper's phone number
 * (excluding the country code).
 */
data class ShopperInsightRequest(
    var email: String,
    var phoneCountryCode: String,
    var phoneNationalNumber: String
)
