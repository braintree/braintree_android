package com.braintreepayments.api.test;

import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.PaymentMethodCreatedListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.Braintree.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.List;

/**
 * Simple listener that allows implementers to only override the methods they need.
 */
public abstract class AbstractBraintreeListener implements PaymentMethodsUpdatedListener,
        PaymentMethodCreatedListener, PaymentMethodNonceListener, ErrorListener {

    @Override
    public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {}

    @Override
    public void onPaymentMethodCreated(PaymentMethod paymentMethod) {}

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {}

    @Override
    public void onUnrecoverableError(Throwable throwable) {
        throw new RuntimeException("An UnrecoverableError occurred: " + throwable.getClass() +
                ": " + throwable.getMessage());
    }

    @Override
    public void onRecoverableError(ErrorWithResponse error) {
        throw new RuntimeException("A RecoverableError occurred: " + error.getMessage());
    }
}
