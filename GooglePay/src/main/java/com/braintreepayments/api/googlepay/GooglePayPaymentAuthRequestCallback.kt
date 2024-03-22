package com.braintreepayments.api.googlepay

/**
 * Callback to handle result from [GooglePayClient.createPaymentAuthRequest]
 */
fun interface GooglePayPaymentAuthRequestCallback {

    fun onGooglePayPaymentAuthRequest(paymentAuthRequest: GooglePayPaymentAuthRequest?)
}
