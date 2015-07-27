package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.List;

public interface PaymentMethodsUpdatedListener extends BraintreeListener {

    /**
     * {@link #onPaymentMethodsUpdated(List)} will be called with a list of {@link PaymentMethod}s
     * as a callback when
     * {@link com.braintreepayments.api.PaymentMethodTokenization#getPaymentMethods(BraintreeFragment)}
     * is called.
     *
     * @param paymentMethods the {@link List} of {@link PaymentMethod}s.
     */
    void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods);
}
