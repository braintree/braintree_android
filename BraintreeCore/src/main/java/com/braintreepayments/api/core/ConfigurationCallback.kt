package com.braintreepayments.api.core

/**
 * Callback for receiving a [Configuration] result.
 */
fun interface ConfigurationCallback {
    /**
     * @param configuration [Configuration]
     * @param error an exception that occurred while fetching configuration
     */
    fun onResult(configuration: Configuration?, error: Exception?)
}
