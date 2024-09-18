package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when a 500 HTTP_INTERNAL_ERROR response is encountered. Indicates an unexpected
 * error from the server.
 */
public class ServerException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    ServerException(String message) {
        super(message);
    }
}
