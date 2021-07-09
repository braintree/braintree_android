package com.braintreepayments.api;

/**
 * Exception thrown when an HTTP request fails.
 */
public class HttpClientException extends Exception {

    HttpClientException(String message) {
        super(message);
    }
}
