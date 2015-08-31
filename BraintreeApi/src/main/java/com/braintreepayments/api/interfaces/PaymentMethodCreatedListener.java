package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.PaymentMethod;

/**
 * Interface that defines callbacks to be called when {@link PaymentMethod}s are created.
 */
public interface PaymentMethodCreatedListener extends BraintreeListener {

    /**
     * {@link #onPaymentMethodCreated} will be called with a new {@link PaymentMethod} has been
     * created.
     *
     * @param paymentMethod the {@link PaymentMethod}.
     */
    void onPaymentMethodCreated(PaymentMethod paymentMethod);
}
