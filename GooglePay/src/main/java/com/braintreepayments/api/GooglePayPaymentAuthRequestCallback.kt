package com.braintreepayments.api

/**
 * Callback to handle result from [GooglePayClient.createPaymentAuthRequest]
 */
fun interface GooglePayPaymentAuthRequestCallback {

    fun onGooglePayPaymentAuthRequest(paymentAuthRequest: GooglePayPaymentAuthRequest?)
}
