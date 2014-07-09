package com.braintreepayments.api.exceptions;

/**
 * Error thrown when the Braintree gateway returns an HTTP_INTERNAL_ERROR.
 * This represents an unexpected error from the Braintree server.
 */
public class ServerException extends BraintreeException {
    public ServerException() {
        super();
    }

    public ServerException(String message) {
        super(message);
    }
}
