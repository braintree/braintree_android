package com.braintreepayments.api;

import android.content.Context;

/**
 * Error class thrown when a {@link PaymentMethodClient#deletePaymentMethod(Context, PaymentMethodNonce, DeletePaymentMethodNonceCallback)}
 * fails to delete a payment method.
 */
public class PaymentMethodDeleteException extends Exception {

    private final PaymentMethodNonce paymentMethodNonce;

    PaymentMethodDeleteException(PaymentMethodNonce paymentMethodNonce, Exception exception) {
        super(exception);
        this.paymentMethodNonce = paymentMethodNonce;
    }

    /**
     * @return The {@link PaymentMethodNonce} that failed to be deleted.
     */
    public PaymentMethodNonce getPaymentMethodNonce() {
        return paymentMethodNonce;
    }
}
