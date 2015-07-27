package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.PaymentMethod;

public interface PaymentMethodCreatedListener extends BraintreeListener {

    /**
     * {@link #onPaymentMethodCreated} will be called with a new {@link PaymentMethod} has been
     * created.
     *
     * @param paymentMethod the {@link PaymentMethod}.
     */
    void onPaymentMethodCreated(PaymentMethod paymentMethod);
}
