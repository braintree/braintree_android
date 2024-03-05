package com.braintreepayments.api

/**
 * Callback for receiving result of [GooglePayClient.tokenize].
 */
fun interface GooglePayTokenizeCallback {

    /**
     * @param googlePayResult [PaymentMethodNonce]
     */
    fun onGooglePayResult(googlePayResult: GooglePayResult?)
}
