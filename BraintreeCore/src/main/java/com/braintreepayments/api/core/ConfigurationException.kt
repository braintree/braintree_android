package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * Error class thrown when a configuration value is invalid
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConfigurationException @JvmOverloads constructor(
    message: String?,
    cause: Throwable? = null
) : BraintreeException(message, cause)
