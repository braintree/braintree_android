package com.braintreepayments.test;

import android.app.Activity;

import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;

/**
 * Dummy activity that implements a {@link PaymentMethodNonceListener} for the sake of testing
 */
public class TestListenerActivity extends Activity implements PaymentMethodNonceListener {
    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
    }
}
