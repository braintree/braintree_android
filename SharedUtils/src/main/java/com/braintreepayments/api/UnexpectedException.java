package com.braintreepayments.api;

/**
 * Exception thrown when an unrecognized error occurs while communicating with a server. This may
 * represent an {@link java.io.IOException} or an unexpected HTTP response.
 */
public class UnexpectedException extends Exception {

    public UnexpectedException(String message) {
        super(message);
    }
}
