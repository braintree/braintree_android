package com.braintreepayments.api

/**
 * Implement this interface to provide an asynchronous way for [BraintreeClient] to fetch
 * a client token from your server when it is needed.
 */
fun interface ClientTokenProvider {
    /**
     * Method used by [BraintreeClient] to fetch a client token.
     * @param callback [ClientTokenCallback] to invoke to notify [BraintreeClient] of success (or
     * failure) when fetching a client token
     */
    fun getClientToken(callback: ClientTokenCallback)
}
