package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when an unrecognized error occurs while communicating with a server. This may
 * represent an {@link java.io.IOException} or an unexpected HTTP response.
 */
public class UnexpectedException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public UnexpectedException(String message) {
        super(message);
    }

    UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
