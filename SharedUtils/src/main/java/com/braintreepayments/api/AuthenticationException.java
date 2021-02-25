package com.braintreepayments.api;

/**
 * Exception thrown when a 401 HTTP_UNAUTHORIZED response is encountered. Indicates authentication
 * has failed in some way.
 */
public class AuthenticationException extends Exception {

    AuthenticationException(String message) {
        super(message);
    }
}
