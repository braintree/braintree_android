package com.braintreepayments.api;

/**
 * Exception thrown when an error occurs while interacting with Android shared preferences.
 */
public class BraintreeSharedPreferencesException  extends Exception {

    BraintreeSharedPreferencesException(String message) {
        super(message);
    }

    BraintreeSharedPreferencesException(String message, Throwable cause) {
        super(message, cause);
    }
}
