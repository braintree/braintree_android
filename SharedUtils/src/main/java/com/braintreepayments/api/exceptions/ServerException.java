package com.braintreepayments.api.exceptions;

/**
 * Exception thrown when a 500 HTTP_INTERNAL_ERROR response is encountered. Indicates an unexpected
 * error from the server.
 */
public class ServerException extends Exception {

    public ServerException(String message) {
        super(message);
    }
}
