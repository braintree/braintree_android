package com.braintreepayments.api

import java.io.IOException

/**
 * Parent class for exceptions encountered when using the SDK.
 */
open class BraintreeException : IOException {
    internal constructor() : super()
    internal constructor(message: String?) : super(message)
    internal constructor(message: String?, cause: Throwable?) : super(message, cause)
}
