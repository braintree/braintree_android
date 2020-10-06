package com.braintreepayments.api.exceptions;

/**
 * Thrown when a payment method isn't available with a developer friendly explanation on why
 * it isn't available.
 */
public class PaymentMethodNotAvailableException extends BraintreeException {

    public PaymentMethodNotAvailableException(String message) {
        super(message);
    }
}
