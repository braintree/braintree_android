package com.braintreepayments.api

import androidx.annotation.RestrictTo

/**
 * Error class thrown when a configuration value is invalid
 */
open class ConfigurationException @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @JvmOverloads constructor(
    message: String?,
    cause: Throwable? = null
) : BraintreeException(message, cause)
