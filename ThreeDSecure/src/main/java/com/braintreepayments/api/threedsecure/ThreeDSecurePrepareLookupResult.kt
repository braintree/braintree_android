package com.braintreepayments.api.threedsecure

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#prepareLookup(Context, ThreeDSecureRequest,
 * ThreeDSecurePrepareLookupCallback)}.
 */
sealed class ThreeDSecurePrepareLookupResult {

    /**
     * The lookup was successfully prepared. The [clientData] JSON string of client data can be sent
     * to server for lookup
     */
    class Success internal constructor(
        val request: ThreeDSecureRequest,
        val clientData: String
    ) : ThreeDSecurePrepareLookupResult()

    /**
     * There was an [error] preparing the lookup
     */
    class Failure internal constructor(val error: Exception) : ThreeDSecurePrepareLookupResult()
}
