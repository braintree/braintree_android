package com.braintreepayments.api

/**
 * Callback for receiving result of [PayPalClient.tokenize].
 */
fun interface PayPalTokenizeCallback {

    /**
     * @param payPalResult a success, failure, or cancel result from the PayPal flow
     */
    fun onPayPalResult(payPalResult: PayPalResult)
}
