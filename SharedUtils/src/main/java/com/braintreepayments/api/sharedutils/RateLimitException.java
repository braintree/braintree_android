package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when a 429 HTTP_TOO_MANY_REQUESTS response is encountered. Indicates the client has hit a request
 * limit and should wait a period of time and try again.
 */
public class RateLimitException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    RateLimitException(String message) {
        super(message);
    }
}
