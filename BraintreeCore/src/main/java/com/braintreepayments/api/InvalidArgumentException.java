package com.braintreepayments.api;

/**
 * Error thrown when arguments provided to a method are invalid.
 */
public class InvalidArgumentException extends Exception {
    InvalidArgumentException(String message) {
        super(message);
    }
}
