package com.braintreepayments.api.exceptions;

/**
 * Error class thrown when a 403 HTTP_FORBIDDEN is encountered from the Braintree gateway.
 */
public class AuthorizationException extends BraintreeException {
    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException() {
        super();
    }
}
