package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * Callback for receiving result of [BraintreeClient.getConfiguration].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface ConfigurationCallback {
    /**
     * @param configuration [Configuration]
     * @param error an exception that occurred while fetching configuration
     */
    fun onResult(configuration: Configuration?, error: Exception?)
}
