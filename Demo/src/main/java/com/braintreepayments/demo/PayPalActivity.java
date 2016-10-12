package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.PayPalOverrides;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import java.util.Collections;

public class PayPalActivity extends BaseActivity implements ConfigurationListener,
        PaymentMethodNonceCreatedListener, BraintreeErrorListener {

    private String mDeviceData;

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
                PayPalOneTouchCore.isWalletAppInstalled(this)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PayPalOverrides.setFuturePaymentsOverride(false);
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
        PayPalOverrides.setFuturePaymentsOverride(true);
        setProgressBarIndeterminateVisibility(true);
        PayPal.authorizeAccount(mBraintreeFragment);
    }

    public void launchFuturePaymentAddressScope(View v) {
        PayPalOverrides.setFuturePaymentsOverride(true);
        setProgressBarIndeterminateVisibility(true);
        PayPal.authorizeAccount(mBraintreeFragment, Collections.singletonList(PayPal.SCOPE_ADDRESS));
    }

    public void launchSinglePayment(View v) {
        setProgressBarIndeterminateVisibility(true);
        PayPal.requestOneTimePayment(mBraintreeFragment, getPayPalRequest("1.00"));
    }

    public void launchBillingAgreement(View v) {
        setProgressBarIndeterminateVisibility(true);
        PayPal.requestBillingAgreement(mBraintreeFragment, getPayPalRequest(null));
    }

    @Override
    public void onCancel(int requestCode) {
        super.onCancel(requestCode);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onConfigurationFetched(Configuration configuration) {
        if (getIntent().getBooleanExtra(MainActivity.EXTRA_COLLECT_DEVICE_DATA, false)) {
            DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
                @Override
                public void onResponse(String deviceData) {
                    mDeviceData = deviceData;
                }
            });
        }
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(MainActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce)
                .putExtra(MainActivity.EXTRA_DEVICE_DATA, mDeviceData);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onError(Exception error) {
        super.onError(error);
        setProgressBarIndeterminateVisibility(false);
    }

    private PayPalRequest getPayPalRequest(@Nullable String amount) {
        PayPalRequest request = new PayPalRequest(amount);
        if (Settings.isPayPalIntentSaleEnabled(this)) {
            request.intent(PayPalRequest.INTENT_SALE);
        }

        if (Settings.isPayPalUseractionCommitEnabled(this)) {
            request.userAction(PayPalRequest.USER_ACTION_COMMIT);
        }

        return request;
    }
}
