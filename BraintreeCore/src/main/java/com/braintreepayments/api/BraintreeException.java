package com.braintreepayments.api;

import java.io.IOException;

/**
 * Parent class for exceptions encountered when using the SDK.
 */
public class BraintreeException extends IOException {

    BraintreeException(String message, Throwable cause) {
        super(message, cause);
    }

    BraintreeException(String message) {
        super(message);
    }

    BraintreeException() {
        super();
    }
}
