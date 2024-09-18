package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * Error class thrown when a configuration value is invalid
 */
class ConfigurationException @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    message: String?,
    cause: Throwable? = null
) : BraintreeException(message, cause)
