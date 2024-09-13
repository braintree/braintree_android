package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when an HTTP request fails.
 */
public class HttpClientException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    HttpClientException(String message) {
        super(message);
    }
}
