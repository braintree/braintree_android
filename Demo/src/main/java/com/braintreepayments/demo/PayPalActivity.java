package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import java.util.Collections;

public class PayPalActivity extends BaseActivity implements PaymentMethodNonceCreatedListener,
        BraintreeErrorListener {

    private TextView mPayPalAppIndicator;
    private Button mBillingAgreementButton;
    private Button mFuturePaymentAddressScopeButton;
    private Button mFuturePaymentButton;
    private Button mSinglePaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.paypal_activity);
        setUpAsBack();

        mPayPalAppIndicator = (TextView) findViewById(R.id.paypal_wallet_app_indicator);
        mBillingAgreementButton = (Button) findViewById(R.id.paypal_billing_agreement_button);
        mFuturePaymentAddressScopeButton = (Button) findViewById(R.id.paypal_future_payment_address_scope_button);
        mFuturePaymentButton = (Button) findViewById(R.id.paypal_future_payment_button);
        mSinglePaymentButton = (Button) findViewById(R.id.paypal_single_payment_button);

        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPayPalAppIndicator.setText(getString(R.string.paypal_wallet_available,
                PayPalOneTouchCore.isWalletAppInstalled(this, Settings.isPayPalSignatureVerificationDisabled(this))));
    }

    @Override
    protected void reset() {
        setProgressBarIndeterminateVisibility(true);
        enableButtons(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }

        enableButtons(true);
        setProgressBarIndeterminateVisibility(false);
    }

    private void enableButtons(boolean enabled) {
        mBillingAgreementButton.setEnabled(enabled);
        mFuturePaymentAddressScopeButton.setEnabled(enabled);
        mFuturePaymentButton.setEnabled(enabled);
        mSinglePaymentButton.setEnabled(enabled);
    }

    public void launchFuturePayment(View v) {
        setProgressBarIndeterminateVisibility(true);
        PayPal.authorizeAccount(mBraintreeFragment);
    }

    public void launchFuturePaymentAddressScope(View v) {
        setProgressBarIndeterminateVisibility(true);
        PayPal.authorizeAccount(mBraintreeFragment, Collections.singletonList(PayPal.SCOPE_ADDRESS));
    }

    public void launchSinglePayment(View v) {
        setProgressBarIndeterminateVisibility(true);
        PayPal.requestOneTimePayment(mBraintreeFragment, new PayPalRequest("1.00"));
    }

    public void launchBillingAgreement(View v) {
        setProgressBarIndeterminateVisibility(true);
        PayPal.requestBillingAgreement(mBraintreeFragment, new PayPalRequest());
    }

    @Override
    public void onCancel(int requestCode) {
        super.onCancel(requestCode);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce));
        finish();
    }

    @Override
    public void onError(Exception error) {
        super.onError(error);
        setProgressBarIndeterminateVisibility(false);
    }
}
