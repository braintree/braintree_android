package com.braintreepayments.api

import org.json.JSONObject

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
) {
    fun toJson(): JSONObject = JSONObject().apply {
            put("countryCode", countryCode)
            put("nationalNumber", nationalNumber)
        }
}

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

    private fun toJson(email: String? = null, phone: BuyerPhone? = null): String {
        return JSONObject().apply {
            put("customer", JSONObject().apply {
                putOpt("email", email)
                putOpt("phone", phone?.toJson())
            })
        }.toString()
    }

    fun toJson(): String {
        return when (this) {
            is Email -> toJson(email = email)
            is Phone -> toJson(phone = phone)
            is EmailAndPhone -> toJson(email = email, phone = phone)
        }
    }
}
