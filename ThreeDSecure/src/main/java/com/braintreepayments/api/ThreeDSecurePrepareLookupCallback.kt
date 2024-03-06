package com.braintreepayments.api

/**
 * Callback for receiving result of [ThreeDSecureClient.prepareLookup].
 */
interface ThreeDSecurePrepareLookupCallback {

    /**
     * @param prepareLookupResult [ThreeDSecurePrepareLookupResult]
     */
    fun onPrepareLookupResult(prepareLookupResult: ThreeDSecurePrepareLookupResult?)
}
