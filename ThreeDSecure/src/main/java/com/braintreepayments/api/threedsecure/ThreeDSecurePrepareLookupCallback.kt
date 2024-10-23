package com.braintreepayments.api.threedsecure

/**
 * Callback for receiving result of [ThreeDSecureClient.prepareLookup].
 */
fun interface ThreeDSecurePrepareLookupCallback {

    /**
     * @param prepareLookupResult [ThreeDSecurePrepareLookupResult]
     */
    fun onPrepareLookupResult(prepareLookupResult: ThreeDSecurePrepareLookupResult?)
}
