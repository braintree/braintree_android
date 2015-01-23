package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.PaymentButton;

public class PaymentButtonActivity extends Activity implements PaymentMethodNonceListener {

    private Braintree mBraintree;
    private PaymentButton mPaymentButton;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.payment_button);

        mPaymentButton = (PaymentButton) findViewById(R.id.payment_button);

        mBraintree = Braintree.getInstance(this,
                getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN));
        mBraintree.addListener(this);
        mPaymentButton.initialize(this, mBraintree);
    }

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce));
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == PaymentButton.REQUEST_CODE) {
            mPaymentButton.onActivityResult(requestCode, responseCode, data);
        }
    }
}
