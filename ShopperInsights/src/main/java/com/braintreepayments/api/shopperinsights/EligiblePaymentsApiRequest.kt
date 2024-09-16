package com.braintreepayments.api.shopperinsights

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data class representing a request for find eligible payments api.
 *
 * @property request The request given to us by the merchant
 * @property currencyCode The currency code
 * @property countryCode The country code
 * @property accountDetails Include account details
 * @property constraintType The constraint type
 * @property paymentSources Payment sources, ie. PAYPAL VENMO
 *
 * [currencyCode] [countryCode] are needed for Venmo recommended results
 */
@OptIn(ExperimentalBetaApi::class)
internal data class EligiblePaymentsApiRequest(
    val request: ShopperInsightsRequest,
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

        private const val KEY_COUNTRY_CODE = "country_code"
        private const val KEY_NATIONAL_NUMBER = "national_number"
        private const val KEY_CUSTOMER = "customer"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_PURCHASE_UNITS = "purchase_units"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_CURRENCY_CODE = "currency_code"
        private const val KEY_PREFERENCES = "preferences"
        private const val KEY_INCLUDE_ACCOUNT_DETAILS = "include_account_details"
        private const val KEY_PAYMENT_SOURCE_CONSTRAINT = "payment_source_constraint"
        private const val KEY_CONSTRAINT_TYPE = "constraint_type"
        private const val KEY_PAYMENT_SOURCES = "payment_sources"
    }
}
