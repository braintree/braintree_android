package com.braintreepayments.api;

/**
 * Exception thrown when a 403 HTTP_FORBIDDEN response is encountered. Indicates the current
 * authorization does not have permission to make the request.
 */
public class AuthorizationException extends Exception {

    AuthorizationException(String message) {
        super(message);
    }
}
