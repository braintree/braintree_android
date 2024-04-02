package com.braintreepayments.api

/**
 * Callback for receiving result of [ThreeDSecureClient.createPaymentAuthRequest]
 */
internal fun interface ThreeDSecureResultCallback {

    /**
     * @param threeDSecureParams [ThreeDSecureParams]
     * @param error an exception that occurred while processing a 3D Secure result
     */
    fun onThreeDSecureResult(threeDSecureParams: ThreeDSecureParams?, error: Exception?)
}
