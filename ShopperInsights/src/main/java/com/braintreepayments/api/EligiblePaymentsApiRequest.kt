package com.braintreepayments.api

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data class representing a request for find eligible payments api.
 *
 * @property request The request given to us by the merchant
 * @property merchantId The merchant's ID
 * @property currencyCode The currency code
 * @property countryCode The country code
 * @property accountDetails Include account details
 * @property constraintType The constraint type
 * @property paymentSources Payment sources, ie. PAYPAL VENMO
 *
 * [merchantId] [currencyCode] [countryCode] are needed for Venmo recommended results
 *
 */
@UnstableApi
internal data class EligiblePaymentsApiRequest(
    val request: ShopperInsightsRequest,
    val merchantId: String,
    val currencyCode: String,
    val countryCode: String,
    val accountDetails: Boolean,
    val constraintType: String,
    val paymentSources: List<String>
) {
    companion object {
        fun EligiblePaymentsApiRequest.toJson(): String {
            val jsonPaymentSources = JSONArray()
            for (source in paymentSources) {
                jsonPaymentSources.put(source)
            }
            return JSONObject().apply {
                put(KEY_CUSTOMER, JSONObject().apply {
                    putOpt(KEY_EMAIL, request.email)
                    put(KEY_COUNTRY_CODE, countryCode)
                    request.phone?.let {
                        put(KEY_PHONE, JSONObject().apply {
                            put(KEY_COUNTRY_CODE, it.countryCode)
                            put(KEY_NATIONAL_NUMBER, it.nationalNumber)
                        })
                    }
                })
                put(KEY_PURCHASE_UNITS, JSONArray().apply {
                    put(JSONObject().apply {
                        put(KEY_AMOUNT, JSONObject().apply {
                            put(KEY_CURRENCY_CODE, currencyCode)
                        })
                    })
                })
                put(KEY_PREFERENCES, JSONObject().apply {
                    put(KEY_INCLUDE_ACCOUNT_DETAILS, accountDetails)
                    put(KEY_PAYMENT_SOURCE_CONSTRAINT, JSONObject().apply {
                        put(KEY_CONSTRAINT_TYPE, constraintType)
                        put(KEY_PAYMENT_SOURCES, jsonPaymentSources)
                    })
                })
            }.toString()
        }

        internal const val KEY_COUNTRY_CODE = "country_code"
        internal const val KEY_NATIONAL_NUMBER = "national_number"
        internal const val KEY_CUSTOMER = "customer"
        internal const val KEY_EMAIL = "email"
        internal const val KEY_PHONE = "phone"
        internal const val KEY_PURCHASE_UNITS = "purchase_units"
        internal const val KEY_AMOUNT = "amount"
        internal const val KEY_CURRENCY_CODE = "currency_code"
        internal const val KEY_PREFERENCES = "preferences"
        internal const val KEY_INCLUDE_ACCOUNT_DETAILS = "include_account_details"
        internal const val KEY_PAYMENT_SOURCE_CONSTRAINT = "payment_source_constraint"
        internal const val KEY_CONSTRAINT_TYPE = "constraint_type"
        internal const val KEY_PAYMENT_SOURCES = "payment_sources"
    }
}
