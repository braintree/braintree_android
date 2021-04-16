package com.braintreepayments.api;

import android.content.Context;

/**
 * Error class thrown when a {@link PaymentMethodClient#deletePaymentMethod(Context, BraintreeNonce, DeletePaymentMethodNonceCallback)}
 * fails to delete a payment method.
 */
public class PaymentMethodDeleteException extends Exception {

    private final BraintreeNonce mPaymentMethodNonce;

    PaymentMethodDeleteException(BraintreeNonce paymentMethodNonce, Exception exception) {
        super(exception);
        mPaymentMethodNonce = paymentMethodNonce;
    }

    /**
     * @return The {@link BraintreeNonce} that failed to be deleted.
     */
    public BraintreeNonce getPaymentMethodNonce() {
        return mPaymentMethodNonce;
    }
}
