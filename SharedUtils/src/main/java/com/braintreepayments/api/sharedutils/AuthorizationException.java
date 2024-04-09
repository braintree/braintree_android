package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when a 403 HTTP_FORBIDDEN response is encountered. Indicates the current
 * authorization does not have permission to make the request.
 */
public class AuthorizationException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public AuthorizationException(String message) {
        super(message);
    }
}
