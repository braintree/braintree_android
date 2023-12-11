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
    companion object {
        internal const val KEY_COUNTRY_CODE = "countryCode"
        internal const val KEY_NATIONAL_NUMBER = "nationalNumber"
    }

    fun toJson(): JSONObject = JSONObject().apply {
            put(KEY_COUNTRY_CODE, countryCode)
            put(KEY_NATIONAL_NUMBER, nationalNumber)
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

    fun toJson(): String {
        return when (this) {
            is Email -> toJson(email = email)
            is Phone -> toJson(phone = phone)
            is EmailAndPhone -> toJson(email = email, phone = phone)
        }
    }

    private fun toJson(email: String? = null, phone: BuyerPhone? = null): String {
        return JSONObject().apply {
            put(KEY_CUSTOMER, JSONObject().apply {
                putOpt(KEY_EMAIL, email)
                putOpt(KEY_PHONE, phone?.toJson())
            })
        }.toString()
    }

    companion object {
        internal const val KEY_CUSTOMER = "customer"
        internal const val KEY_EMAIL = "email"
        internal const val KEY_PHONE = "phone"
    }
}
