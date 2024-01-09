package com.braintreepayments.api

import org.json.JSONArray
import org.json.JSONObject

internal class ShoppingInsightsCreateBody {
    fun execute(request: ShopperInsightsApiRequest): String {
        return request.toJson()
    }

    private fun ShopperInsightsApiRequest.toJson(): String {
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
                    put(KEY_PAYEE, JSONObject().apply {
                        put(KEY_MERCHANT_ID, merchantId)
                    })
                    put(KEY_AMOUNT, JSONObject().apply {
                        put(KEY_CURRENCY_CODE, currencyCode)
                    })
                })
            })
            put(KEY_PREFERENCES, JSONObject().apply {
                put(KEY_INCLUDE_ACCOUNT_DETAILS, accountDetails)
                put(KEY_INCLUDE_VAULT_TOKENS, vaultTokens)
                put(KEY_PAYMENT_SOURCE_CONSTRAINT, JSONObject().apply {
                    put(KEY_CONSTRAINT_TYPE, constraintType)
                    put(KEY_PAYMENT_SOURCES, jsonPaymentSources)
                })
            })
        }.toString()
    }

    companion object {
        internal const val KEY_COUNTRY_CODE = "country_code"
        internal const val KEY_NATIONAL_NUMBER = "national_number"
        internal const val KEY_CUSTOMER = "customer"
        internal const val KEY_EMAIL = "email"
        internal const val KEY_PHONE = "phone"
        internal const val KEY_PURCHASE_UNITS = "purchase_units"
        internal const val KEY_PAYEE = "payee"
        internal const val KEY_AMOUNT = "amount"
        internal const val KEY_MERCHANT_ID = "merchant_id"
        internal const val KEY_CURRENCY_CODE = "currency_code"
        internal const val KEY_PREFERENCES = "preferences"
        internal const val KEY_INCLUDE_ACCOUNT_DETAILS = "include_account_details"
        internal const val KEY_INCLUDE_VAULT_TOKENS = "include_vault_tokens"
        internal const val KEY_PAYMENT_SOURCE_CONSTRAINT = "payment_source_constraint"
        internal const val KEY_CONSTRAINT_TYPE = "constraint_type"
        internal const val KEY_PAYMENT_SOURCES = "payment_sources"
    }
}
