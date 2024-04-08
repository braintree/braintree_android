package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

/**
 * Exception thrown when a 422 HTTP_UNPROCESSABLE_ENTITY response is encountered. Indicates the
 * request was invalid in some way.
 */
public class UnprocessableEntityException extends Exception {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
