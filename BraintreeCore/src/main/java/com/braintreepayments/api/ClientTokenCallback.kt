package com.braintreepayments.api

/**
 * Callback used to communicate [ClientTokenProvider.getClientToken] result
 * back to [BraintreeClient].
 */
interface ClientTokenCallback {
    /**
     * Invoke this method once a client token has been successfully fetched from the merchant server.
     * @param clientToken Client token fetched from merchant server
     */
    fun onSuccess(clientToken: String)

    /**
     * Invoke this method when an error occurs fetching the client token
     * @param error An error describing the cause of the client token fetch error
     */
    fun onFailure(error: Exception)
}
