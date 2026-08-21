package com.braintreepayments.api.paypalsavedpaymentmethod

/**
 * Assembles the PayPal Pay Later / Credit presentment messaging URL for the merchant's configured
 * environment.
 */
internal object PayPalCreditMessagingUrlAssembler {

    private const val PRODUCTION_BASE_URL = "https://api.paypal.com"
    private const val SANDBOX_BASE_URL = "https://api.sandbox.paypal.com"
    private const val CREDIT_PRESENTMENT_MESSAGES_PATH = "/v2/credit/fetch-presentment-messages"

    fun assembleURL(environment: String?): String {
        val baseUrl = when (environment) {
            "production" -> PRODUCTION_BASE_URL
            else -> SANDBOX_BASE_URL
        }
        return "$baseUrl$CREDIT_PRESENTMENT_MESSAGES_PATH"
    }
}