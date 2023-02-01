package com.braintreepayments.api

import androidx.annotation.RestrictTo
import java.io.IOException

/**
 * Parent class for exceptions encountered when using the SDK.
 */
open class BraintreeException : IOException {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor() : super()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(message: String?) : super(message)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
