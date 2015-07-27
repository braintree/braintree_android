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
import com.braintreepayments.api.CardTokenizer;
import com.braintreepayments.api.PaymentButton;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;

import java.util.Collections;

public class CustomFormActivity extends Activity implements PaymentMethodCreatedListener,
        OnClickListener {

    private static final int ANDROID_PAY_REQUEST_CODE = 1;

    private BraintreeFragment mBraintreeFragment;
    private Cart mCart;
    private boolean mIsBillingAgreement;
    private EditText mCardNumber;
    private EditText mExpirationDate;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.custom);

        PaymentButton paymentButton = (PaymentButton) findViewById(R.id.payment_button);
        mCardNumber = (EditText) findViewById(R.id.card_number);
        mExpirationDate = (EditText) findViewById(R.id.card_expiration_date);

        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this,
                    getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN));
        } catch (InvalidArgumentException e) {
            Intent intent = new Intent()
                    .putExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE, e.getMessage());
            setResult(RESULT_FIRST_USER, intent);
            finish();
            return;
        }

        mCart = getIntent().getParcelableExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART);
        mIsBillingAgreement = getIntent().getBooleanExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT, false);
        boolean shippingAddressRequired = getIntent().getBooleanExtra("shippingAddressRequired", false);
        boolean phoneNumberRequired = getIntent().getBooleanExtra("phoneNumberRequired", false);
        paymentButton.setAndroidPayOptions(mCart, mIsBillingAgreement, shippingAddressRequired,
                phoneNumberRequired, ANDROID_PAY_REQUEST_CODE);

        boolean payPalAddressScopeRequested = getIntent().getBooleanExtra("payPalAddressScopeRequested", false);
        if (payPalAddressScopeRequested) {
            paymentButton.setAdditionalPayPalScopes(
                    Collections.singletonList(PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS));
        }

        paymentButton.initialize(mBraintreeFragment);
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

        CardTokenizer.tokenize(mBraintreeFragment, cardBuilder);
    }

    @Override
    public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD, paymentMethod));
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

                    FullWalletRequest.Builder fullWalletRequestBuilder = FullWalletRequest.newBuilder()
                            .setGoogleTransactionId(googleTransactionId);

                    if (!mIsBillingAgreement) {
                        fullWalletRequestBuilder.setCart(mCart);
                    }

                    Wallet.Payments.loadFullWallet(mBraintreeFragment.getGoogleApiClient(),
                            fullWalletRequestBuilder.build(), ANDROID_PAY_REQUEST_CODE);
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
