package com.braintreepayments.api

/**
 * Callback for receiving result of
 * [LocalPaymentClient.createPaymentAuthRequest].
 */
fun interface LocalPaymentAuthCallback {
    /**
     * @param paymentAuthRequest a request used to launch the PayPal web authentication flow
     */
    fun onLocalPaymentAuthRequest(paymentAuthRequest: LocalPaymentAuthRequest)
}
