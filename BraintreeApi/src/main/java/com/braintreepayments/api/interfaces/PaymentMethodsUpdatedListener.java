package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.TokenizationClient;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.List;

/**
 * Interface that defines callbacks to be called when existing {@link PaymentMethod}s are fetched.
 */
public interface PaymentMethodsUpdatedListener extends BraintreeListener {

    /**
     * {@link #onPaymentMethodsUpdated(List)} will be called with a list of {@link PaymentMethod}s
     * as a callback when
     * {@link TokenizationClient#getPaymentMethods(BraintreeFragment)}
     * is called.
     *
     * @param paymentMethods the {@link List} of {@link PaymentMethod}s.
     */
    void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods);
}
