package com.braintreepayments.api.threedsecure

/**
 * Callback for receiving the results via [ThreeDSecureLauncher]
 */
fun interface ThreeDSecureLauncherCallback {

    fun onThreeDSecurePaymentAuthResult(paymentAuthResult: ThreeDSecurePaymentAuthResult)
}
