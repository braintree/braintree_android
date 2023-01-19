package com.braintreepayments.api

import androidx.annotation.RestrictTo
import java.lang.Exception

/**
 * Error thrown when arguments provided to a method are invalid.
 */
open class InvalidArgumentException : Exception {

    /**
     * Ref: https://github.com/Kotlin/dokka/issues/1953
     * @param message the error message
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(message: String?) : super(message)
}
