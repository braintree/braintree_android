package com.braintreepayments.api

/**
 * Error class thrown when a configuration value is invalid
 */
open class ConfigurationException : BraintreeException {
    internal constructor(message: String?) : super(message)
    internal constructor(message: String?, cause: Throwable?) : super(message, cause)
}
