package com.braintreepayments.api.paypal

/**
 * Callback for receiving result of [PayPalClient.createPaymentAuthRequest].
 */
fun interface PayPalPaymentAuthCallback {

    /**
     * @param paymentAuthRequest a request used to launch the PayPal web authentication flow
     */
    fun onPayPalPaymentAuthRequest(paymentAuthRequest: PayPalPaymentAuthRequest?)
}
