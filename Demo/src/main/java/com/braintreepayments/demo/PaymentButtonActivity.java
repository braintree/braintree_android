package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.PaymentButton;
import com.google.android.gms.wallet.Cart;

public class PaymentButtonActivity extends Activity implements PaymentMethodNonceListener,
        BraintreeSetupFinishedListener {

    private PaymentButton mPaymentButton;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.payment_button);

        mPaymentButton = (PaymentButton) findViewById(R.id.payment_button);

        Braintree.setup(this, getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN),
                this);
    }

    @Override
    public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
            String errorMessage, Exception exception) {
        if (setupSuccessful) {
            braintree.addListener(this);
            braintree.setCart((Cart) getIntent().getParcelableExtra(BraintreePaymentActivity.EXTRA_CART));
            mPaymentButton.initialize(this, braintree);
        } else {
            Intent intent = new Intent()
                    .putExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE, errorMessage);
            setResult(RESULT_FIRST_USER, intent);
            finish();
        }
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
