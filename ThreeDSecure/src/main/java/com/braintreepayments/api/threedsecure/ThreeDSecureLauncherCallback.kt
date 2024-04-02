package com.braintreepayments.api

/**
 * Callback for receiving the results via [ThreeDSecureLauncher]
 */
fun interface ThreeDSecureLauncherCallback {

    fun onThreeDSecurePaymentAuthResult(paymentAuthResult: ThreeDSecurePaymentAuthResult)
}
