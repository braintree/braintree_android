package com.braintreepayments.api

import androidx.annotation.RestrictTo
import java.io.IOException

/**
 * Parent class for exceptions encountered when using the SDK.
 */
open class BraintreeException @JvmOverloads @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    message: String? = null,
    cause: Throwable? = null
) : IOException(message, cause)
