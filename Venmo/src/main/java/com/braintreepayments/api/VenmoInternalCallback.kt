package com.braintreepayments.api

/**
 * Callback for receiving result of [VenmoClient.tokenize].
 */
internal fun interface VenmoInternalCallback {

    /**
     * @param venmoAccountNonce [VenmoAccountNonce]
     * @param error an exception that occurred while processing a Venmo result
     */
    fun onResult(venmoAccountNonce: VenmoAccountNonce?, error: Exception?)
}
