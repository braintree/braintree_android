package com.braintreepayments.api

import androidx.annotation.RestrictTo

/**
 * Error class thrown when a configuration value is invalid
 */
open class ConfigurationException : BraintreeException {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(message: String?) : super(message)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
