package com.braintreepayments.api

/**
 * Used to receive the result of [ThreeDSecureClient.createPaymentAuthRequest]
 */
fun interface ThreeDSecurePaymentAuthRequestCallback {

    fun onThreeDSecurePaymentAuthRequest(paymentAuthRequest: ThreeDSecurePaymentAuthRequest?)
}
