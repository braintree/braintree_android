package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * Error thrown when arguments provided to a method are invalid.
 *
 * @param message the error message
 */
class InvalidArgumentException
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(message: String?) : Exception(message)
