package com.braintreepayments.api;

/**
 * Exception thrown when an HTTP request fails.
 */
public class HTTPClientException extends Exception {

    HTTPClientException(String message) {
        super(message);
    }
}
