package com.braintreepayments.api.threedsecure

/**
 * Used to receive the result of [ThreeDSecureClient.createPaymentAuthRequest]
 */
fun interface ThreeDSecurePaymentAuthRequestCallback {

    fun onThreeDSecurePaymentAuthRequest(paymentAuthRequest: ThreeDSecurePaymentAuthRequest)
}
