package com.braintreepayments.api.exceptions;

import java.io.IOException;

/**
 * Parent class for exceptions encountered when using the SDK.
 */
public class BraintreeException extends IOException {

    public BraintreeException(String message, Throwable cause) {
        super(message, cause);
    }

    public BraintreeException(String message) {
        super(message);
    }

    public BraintreeException() {
        super();
    }
}
