package com.braintreepayments.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.BraintreeBroadcastManager;
import com.braintreepayments.api.BraintreeBrowserSwitchActivity;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.PaymentButton;

public class PaymentButtonActivity extends Activity implements PaymentMethodNonceListener {

    private Braintree mBraintree;
    private PaymentButton mPaymentButton;

    private BroadcastReceiver mReceiveBraintreeMessages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action
                    .equalsIgnoreCase(BraintreeBrowserSwitchActivity.BROADCAST_BROWSER_COMPLETED)) {
                mPaymentButton.onActivityResult(PaymentButton.REQUEST_CODE, intent.getIntExtra(
                        BraintreeBrowserSwitchActivity.BROADCAST_BROWSER_EXTRA_RESULT,
                        Activity.RESULT_OK),
                        intent);
            }
        }
    };

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.payment_button);

        BraintreeBroadcastManager.getInstance(this).registerReceiver(mReceiveBraintreeMessages,
                new IntentFilter(
                        BraintreeBrowserSwitchActivity.BROADCAST_BROWSER_COMPLETED));

        mPaymentButton = (PaymentButton) findViewById(R.id.payment_button);

        mBraintree = Braintree.getInstance(this,
                getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN));
        mBraintree.addListener(this);
        mPaymentButton.initialize(this, mBraintree);
    }

    protected void onDestroy() {
        BraintreeBroadcastManager.getInstance(this).unregisterReceiver(mReceiveBraintreeMessages);
        super.onDestroy();
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
