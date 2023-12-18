package com.braintreepayments.api

/**
 * Callback for receiving result of[VisaCheckoutClient.createProfileBuilder].
 */
fun interface VisaCheckoutCreateProfileBuilderCallback {
    /**
     * @param profileBuilderResult a [VisaCheckoutProfileBuilderResult]
     */
    fun onVisaCheckoutProfileBuilderResult(profileBuilderResult: VisaCheckoutProfileBuilderResult)
}