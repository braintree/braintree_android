package com.braintreepayments.api.googlepay

/**
 * Callback for receiving result of [GooglePayClient.tokenize].
 */
fun interface GooglePayTokenizeCallback {

    /**
     * @param googlePayResult [PaymentMethodNonce]
     */
    fun onGooglePayResult(googlePayResult: GooglePayResult?)
}
