package com.braintreepayments.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;

public class PayPal extends BaseActivity implements PaymentMethodNonceListener {

    private static final int PAYPAL_REQUEST_CODE = 100;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.paypal);
    }

    public void startPayPal(View v) {
        mBraintree.startPayWithPayPal(this, PAYPAL_REQUEST_CODE);
    }

    @Override
    public void ready(String clientToken) {
        mBraintree = Braintree.getInstance(this, clientToken);
        mBraintree.addListener(this);

        findViewById(R.id.paypal_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onPaymentMethodNonce(String nonce) {
        postNonceToServer(nonce);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mBraintree.finishPayWithPayPal(this, resultCode, data);
                return;
            }
        }

        showDialog("Request code was " + requestCode + ", we were looking for " + PAYPAL_REQUEST_CODE +
            " Result code was " + resultCode + ", we were looking for " + RESULT_OK);
    }
}
