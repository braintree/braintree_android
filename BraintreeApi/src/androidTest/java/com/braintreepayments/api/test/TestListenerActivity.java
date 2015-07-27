package com.braintreepayments.api.test;

import android.app.Activity;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.List;

/**
 * Activity that implements all listeners used by {@link com.braintreepayments.api.BraintreeFragment}
 * for testing.
 */
public class TestListenerActivity extends Activity implements PaymentMethodCreatedListener,
        PaymentMethodsUpdatedListener, BraintreeErrorListener, ConfigurationListener {

    @Override
    public void onUnrecoverableError(Throwable throwable) {
    }

    @Override
    public void onRecoverableError(ErrorWithResponse error) {
    }

    @Override
    public void onConfigurationFetched() {
    }

    @Override
    public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
    }

    @Override
    public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
    }
}
