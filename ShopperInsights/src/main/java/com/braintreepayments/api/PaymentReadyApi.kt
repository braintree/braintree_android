package com.braintreepayments.api

import org.json.JSONObject

// TODO: Implementation, documentation and interface.
internal class PaymentReadyApi {
    fun processRequest(request: ShopperInsightsApiRequest): String = request.toJson()

    private fun ShopperInsightsApiRequest.toJson(): String {
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
            put(KEY_PURCHASE_UNITS, JSONObject().apply {
                put(KEY_PAYEE, JSONObject().apply {
                    put(KEY_MERCHANT_ID, merchantId)
                })
                put(KEY_AMOUNT, JSONObject().apply {
                    put(KEY_CURRENCY_CODE, currencyCode)
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
    }
}
