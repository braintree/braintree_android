package com.braintreepayments.api.threedsecure

/**
 * Used to receive the result of [ThreeDSecureClient.tokenize]
 */
fun interface ThreeDSecureTokenizeCallback {

    fun onThreeDSecureResult(threeDSecureResult: ThreeDSecureResult)
}
