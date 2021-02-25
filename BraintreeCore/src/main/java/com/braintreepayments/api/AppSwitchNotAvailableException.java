package com.braintreepayments.api;

/**
 * Error class thrown when app switch to the corresponding wallet is not possible
 */
public class AppSwitchNotAvailableException extends BraintreeException {

    AppSwitchNotAvailableException(String message) {
        super(message);
    }
}
