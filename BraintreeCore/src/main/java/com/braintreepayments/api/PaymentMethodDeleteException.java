package com.braintreepayments.api;

import android.content.Context;

/**
 * Error class thrown when a {@link PaymentMethodClient#deletePaymentMethod(Context, UntypedPaymentMethodNonce, DeletePaymentMethodNonceCallback)}
 * fails to delete a payment method.
 */
public class PaymentMethodDeleteException extends Exception {

    private final UntypedPaymentMethodNonce mPaymentMethodNonce;

    PaymentMethodDeleteException(UntypedPaymentMethodNonce paymentMethodNonce, Exception exception) {
        super(exception);
        mPaymentMethodNonce = paymentMethodNonce;
    }

    /**
     * @return The {@link UntypedPaymentMethodNonce} that failed to be deleted.
     */
    public UntypedPaymentMethodNonce getPaymentMethodNonce() {
        return mPaymentMethodNonce;
    }
}
