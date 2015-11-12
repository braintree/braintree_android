package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.PaymentButton;
import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

public class CustomFormActivity extends BaseActivity implements PaymentMethodNonceCreatedListener,
        BraintreeErrorListener, OnClickListener {

    private static final int ANDROID_PAY_REQUEST_CODE = 1;

    private Cart mCart;
    private EditText mCardNumber;
    private EditText mExpirationDate;
    private Button mPurchaseButton;

    @Override
    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.custom);
        setUpAsBack();

        mCart = getIntent().getParcelableExtra(MainActivity.EXTRA_ANDROID_PAY_CART);

        mCardNumber = (EditText) findViewById(R.id.card_number);
        mExpirationDate = (EditText) findViewById(R.id.card_expiration_date);
        mPurchaseButton = (Button) findViewById(R.id.purchase_button);

        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected void reset() {
        setProgressBarIndeterminateVisibility(true);
        mPurchaseButton.setEnabled(false);
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
            PaymentButton paymentButton = PaymentButton.newInstance(this,
                    R.id.payment_button_container, paymentRequest);

            paymentButton.setOnClickListener(this);
        } catch (InvalidArgumentException ignored) {
            // already checked via BraintreeFragment.newInstance
        }

        setProgressBarIndeterminateVisibility(false);
        mPurchaseButton.setEnabled(true);
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

        Card.tokenize(mBraintreeFragment, cardBuilder);
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
