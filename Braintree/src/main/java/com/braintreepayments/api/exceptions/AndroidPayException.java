package com.braintreepayments.api.exceptions;

/**
 * Error class thrown when an Android Pay exception is encountered.
 */
public class AndroidPayException extends Exception {

    public AndroidPayException(String message) {
        super(message);
    }
}
