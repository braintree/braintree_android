package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PaymentButton;
import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

public class PaymentButtonActivity extends BaseActivity implements ConfigurationListener,
        PaymentMethodNonceCreatedListener, BraintreeErrorListener, OnClickListener {

    private static final int ANDROID_PAY_REQUEST_CODE = 1;

    private Cart mCart;
    private String mDeviceData;

    @Override
    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.payment_button_activity);
        setUpAsBack();

        mCart = getIntent().getParcelableExtra(MainActivity.EXTRA_ANDROID_PAY_CART);

        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected void reset() {
        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }

        try {
            PaymentRequest paymentRequest = getIntent().getParcelableExtra(MainActivity.EXTRA_PAYMENT_REQUEST);
            paymentRequest.androidPayRequestCode(ANDROID_PAY_REQUEST_CODE);
            PaymentButton paymentButton = PaymentButton.newInstance(this,
                    R.id.payment_button_container, paymentRequest);

            paymentButton.setOnClickListener(this);
        } catch (InvalidArgumentException ignored) {
            // already checked via BraintreeFragment.newInstance
        }

        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onClick(View v) {
        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void onCancel(int requestCode) {
        super.onCancel(requestCode);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onConfigurationFetched(Configuration configuration) {
        if (getIntent().getBooleanExtra(MainActivity.EXTRA_COLLECT_DEVICE_DATA, false)) {
            mDeviceData = DataCollector.collectDeviceData(mBraintreeFragment);
        }
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce)
                .putExtra(BraintreePaymentActivity.EXTRA_DEVICE_DATA, mDeviceData);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onError(Exception error) {
        super.onError(error);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == ANDROID_PAY_REQUEST_CODE) {
                if (data.hasExtra(WalletConstants.EXTRA_MASKED_WALLET)) {
                    String googleTransactionId =
                            ((MaskedWallet) data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET))
                                    .getGoogleTransactionId();

                    final FullWalletRequest fullWalletRequest = FullWalletRequest.newBuilder()
                            .setGoogleTransactionId(googleTransactionId)
                            .setCart(mCart)
                            .build();

                    mBraintreeFragment.getGoogleApiClient(
                            new BraintreeResponseListener<GoogleApiClient>() {
                                @Override
                                public void onResponse(GoogleApiClient googleApiClient) {
                                    Wallet.Payments.loadFullWallet(googleApiClient,
                                            fullWalletRequest,
                                            ANDROID_PAY_REQUEST_CODE);
                                }
                            });
                } else {
                    AndroidPay.tokenize(mBraintreeFragment,
                            (FullWallet) data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET));
                }
            }
        } else {
            setProgressBarIndeterminateVisibility(false);
        }
    }
}
