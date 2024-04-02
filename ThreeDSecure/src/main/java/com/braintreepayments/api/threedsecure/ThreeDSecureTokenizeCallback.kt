package com.braintreepayments.api

/**
 * Used to receive the result of [ThreeDSecureClient.tokenize]
 */
fun interface ThreeDSecureTokenizeCallback {

    fun onThreeDSecureResult(threeDSecureResult: ThreeDSecureResult)
}
