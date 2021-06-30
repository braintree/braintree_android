package com.braintreepayments.api;

/**
 * Error class thrown when a user cancels a payment flow
 */
public class UserCanceledException extends BraintreeException {

    UserCanceledException(String message) {
        super(message);
    }
}
