package com.braintreepayments.api;

/**
 * Error class thrown when a {@link PaymentMethodClient#deletePaymentMethod(BraintreeFragment, PaymentMethodNonce)}
 * fails to delete a payment method.
 */
public class PaymentMethodDeleteException extends Exception {

    private final PaymentMethodNonce mPaymentMethodNonce;

    PaymentMethodDeleteException(PaymentMethodNonce paymentMethodNonce, Exception exception) {
        super(exception);
        mPaymentMethodNonce = paymentMethodNonce;
    }

    /**
     * @return The {@link PaymentMethodNonce} that failed to be deleted.
     */
    public PaymentMethodNonce getPaymentMethodNonce() {
        return mPaymentMethodNonce;
    }
}
