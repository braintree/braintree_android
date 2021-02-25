package com.braintreepayments.api;

/**
 * Exception thrown when a 500 HTTP_INTERNAL_ERROR response is encountered. Indicates an unexpected
 * error from the server.
 */
public class ServerException extends Exception {

    ServerException(String message) {
        super(message);
    }
}
