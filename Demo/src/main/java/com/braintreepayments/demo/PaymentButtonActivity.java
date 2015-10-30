package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.PaymentButton;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.models.PaymentMethod;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import java.util.Collections;

public class PaymentButtonActivity extends Activity implements PaymentMethodCreatedListener,
        OnClickListener {

    private static final int ANDROID_PAY_REQUEST_CODE = 1;

    private BraintreeFragment mBraintreeFragment;
    private Cart mCart;

    @Override
    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.payment_button);

        PaymentButton paymentButton = (PaymentButton) findViewById(R.id.payment_button);

        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this,
                    getIntent().getStringExtra(MainActivity.EXTRA_AUTHORIZATION));
        } catch (InvalidArgumentException e) {
            Intent intent = new Intent()
                    .putExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE, e.getMessage());
            setResult(RESULT_FIRST_USER, intent);
            finish();
            return;
        }

        mCart = getIntent().getParcelableExtra(MainActivity.EXTRA_ANDROID_PAY_CART);

        PaymentRequest paymentRequest = new PaymentRequest()
                .androidPayCart(mCart)
                .androidPayShippingAddressRequired(getIntent().getBooleanExtra(MainActivity.EXTRA_ANDROID_PAY_SHIPPING_ADDRESS_REQUIRED, false))
                .androidPayPhoneNumberRequired(getIntent().getBooleanExtra(MainActivity.EXTRA_ANDROID_PAY_PHONE_NUMBER_REQUIRED, false))
                .androidPayRequestCode(ANDROID_PAY_REQUEST_CODE);

        if (getIntent().getBooleanExtra(MainActivity.EXTRA_PAYPAL_ADDRESS_SCOPE_REQUESTED, false)) {
            paymentRequest.paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS));
        }

        paymentButton.initialize(mBraintreeFragment, paymentRequest);
        paymentButton.setOnClickListener(this);
    }

    @Override
    public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD, paymentMethod));
        finish();
    }

    @Override
    public void onClick(View v) {
        setProgressBarIndeterminate(true);
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
            setProgressBarIndeterminate(false);
        }
    }
}
