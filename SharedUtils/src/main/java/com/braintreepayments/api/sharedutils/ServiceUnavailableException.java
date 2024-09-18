package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when a 503 HTTP_UNAVAILABLE response is encountered. Indicates the server is
 * unreachable or the request timed out.
 */
public class ServiceUnavailableException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    ServiceUnavailableException(String message) {
        super(message);
    }
}
