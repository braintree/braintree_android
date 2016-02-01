package com.braintreepayments.api.exceptions;

import java.io.IOException;

/**
 * Parent class for exceptions encountered when attempting to communicate with the Braintree gateway.
 */
public class BraintreeException extends IOException {
    public BraintreeException(String message) {
        super(message);
    }

    public BraintreeException() {
        super();
    }
}
