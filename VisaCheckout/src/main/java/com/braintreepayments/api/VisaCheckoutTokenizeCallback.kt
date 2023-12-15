package com.braintreepayments.api

/**
 * Callback for receiving result of [VisaCheckoutClient.tokenize].
 */
fun interface VisaCheckoutTokenizeCallback {
    /**
     * @param visaCheckoutResult a [VisaCheckoutResult]
     */
    fun onVisaCheckoutResult(visaCheckoutResult: VisaCheckoutResult)
}