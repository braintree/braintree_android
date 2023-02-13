package com.braintreepayments.api

import androidx.annotation.RestrictTo

// NEXT MAJOR VERSION: remove open modifier
/**
 * Error thrown when arguments provided to a method are invalid.
 */
open class InvalidArgumentException : Exception {

    /**
     * Ref: https://github.com/Kotlin/dokka/issues/1953
     * @param message the error message
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(message: String?) : super(message)
}
