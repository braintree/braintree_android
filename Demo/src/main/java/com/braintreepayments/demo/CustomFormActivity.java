package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.PaymentButton;
import com.braintreepayments.api.TokenizationClient;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.models.CardBuilder;
import com.google.android.gms.common.api.GoogleApiClient;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

public class CustomFormActivity extends Activity implements PaymentMethodNonceCreatedListener,
        OnClickListener {

    private static final int ANDROID_PAY_REQUEST_CODE = 1;

    private BraintreeFragment mBraintreeFragment;
    private Cart mCart;
    private EditText mCardNumber;
    private EditText mExpirationDate;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.custom);

        PaymentButton paymentButton = (PaymentButton) getFragmentManager()
                .findFragmentById(R.id.payment_button);
        mCardNumber = (EditText) findViewById(R.id.card_number);
        mExpirationDate = (EditText) findViewById(R.id.card_expiration_date);

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

        PaymentRequest paymentRequest = getIntent().getParcelableExtra(MainActivity.EXTRA_PAYMENT_REQUEST);

        try {
            paymentButton.setPaymentRequest(paymentRequest);
        } catch (InvalidArgumentException ignored) {
            // already checked via BraintreeFragment.newInstance
        }

        paymentButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        setProgressBarIndeterminateVisibility(true);
    }

    public void onPurchase(View v) {
        setProgressBarIndeterminateVisibility(true);

        CardBuilder cardBuilder = new CardBuilder()
            .cardNumber(mCardNumber.getText().toString())
            .expirationDate(mExpirationDate.getText().toString());

        TokenizationClient.tokenize(mBraintreeFragment, cardBuilder);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce));
        finish();
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
