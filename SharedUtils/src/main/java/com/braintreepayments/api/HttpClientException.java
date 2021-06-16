package com.braintreepayments.api;

// NEXT MAJOR VERSION: rename this class to HTTPClientException to matching acronym casing with other HTTP classes
/**
 * Exception thrown when an HTTP request fails.
 */
public class HttpClientException extends Exception {

    HttpClientException(String message) {
        super(message);
    }
}
