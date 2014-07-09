package com.braintreepayments.api.exceptions;

/**
 * Thrown when an unrecognized error occurs while communicating with the Braintree gateway.
 * This may represent an {@link java.io.IOException} or an unexpected HTTP response.
 */
public class UnexpectedException extends BraintreeException {
    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException() {
        super();
    }
}
