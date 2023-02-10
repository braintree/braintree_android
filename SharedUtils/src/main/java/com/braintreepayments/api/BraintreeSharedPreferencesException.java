package com.braintreepayments.api;

// NEXT MAJOR VERSION: Remove this class
/**
 * Exception thrown when an error occurs while interacting with Android shared preferences.
 * @deprecated
 */
@Deprecated
public class BraintreeSharedPreferencesException  extends Exception {

    BraintreeSharedPreferencesException(String message) {
        super(message);
    }
}
