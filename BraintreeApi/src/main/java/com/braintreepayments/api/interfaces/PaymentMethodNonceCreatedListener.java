package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.PaymentMethodNonce;

/**
 * Interface that defines callbacks to be called when {@link PaymentMethodNonce}s are created.
 */
public interface PaymentMethodNonceCreatedListener extends BraintreeListener {

    /**
     * {@link #onPaymentMethodNonceCreated} will be called with a new {@link PaymentMethodNonce} has been
     * created.
     *
     * @param paymentMethodNonce the {@link PaymentMethodNonce}.
     */
    void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce);
}
