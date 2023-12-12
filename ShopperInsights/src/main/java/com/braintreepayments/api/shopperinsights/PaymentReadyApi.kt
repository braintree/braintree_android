package com.braintreepayments.api.shopperinsights

import org.json.JSONObject

// TODO: Implementation, documentation and interface.
internal class PaymentReadyApi {
    fun processRequest(request: ShopperInsightsRequest): String = request.toJson()

    private fun ShopperInsightsRequest.toJson(): String {
        return JSONObject().apply {
            put(KEY_CUSTOMER, JSONObject().apply {
                putOpt(KEY_EMAIL, email)
                phone?.let {
                    put(KEY_PHONE, JSONObject().apply {
                        put(KEY_COUNTRY_CODE, it.countryCode)
                        put(KEY_NATIONAL_NUMBER, it.nationalNumber)
                    })
                }
            })
        }.toString()
    }

    companion object {
        internal const val KEY_COUNTRY_CODE = "countryCode"
        internal const val KEY_NATIONAL_NUMBER = "nationalNumber"
        internal const val KEY_CUSTOMER = "customer"
        internal const val KEY_EMAIL = "email"
        internal const val KEY_PHONE = "phone"
    }
}
