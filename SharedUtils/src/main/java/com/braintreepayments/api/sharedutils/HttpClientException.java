package com.braintreepayments.api.sharedutils;

/**
 * Exception thrown when an HTTP request fails.
 */
public class HttpClientException extends Exception {

    HttpClientException(String message) {
        super(message);
    }
}
