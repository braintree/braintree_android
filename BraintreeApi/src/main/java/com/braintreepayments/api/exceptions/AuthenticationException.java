package com.braintreepayments.api.exceptions;

/**
 * Error class thrown when a 401 HTTP_UNAUTHORIZED is encountered from the Braintree gateway.
 * Indicates authentication has failed in some way.
 */
public class AuthenticationException extends BraintreeException {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException() {
        super();
    }
}
