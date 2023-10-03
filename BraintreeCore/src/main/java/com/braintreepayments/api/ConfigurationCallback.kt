package com.braintreepayments.api

/**
 * Callback for receiving result of [BraintreeClient.getConfiguration].
 */
fun interface ConfigurationCallback {
    /**
     * @param configuration [Configuration]
     * @param error an exception that occurred while fetching configuration
     */
    fun onResult(configuration: Configuration?, error: Exception?)
}
