package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.Braintree.PaymentMethodCreatedListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.PaymentButton;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.google.android.gms.wallet.Cart;

import java.util.Collections;

public class CustomFormActivity extends Activity implements PaymentMethodCreatedListener,
        PaymentMethodNonceListener, BraintreeSetupFinishedListener {

    private Braintree mBraintree;
    private PaymentButton mPaymentButton;
    private EditText mCardNumber;
    private EditText mExpirationDate;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.custom);

        mPaymentButton = (PaymentButton) findViewById(R.id.payment_button);
        mCardNumber = (EditText) findViewById(R.id.card_number);
        mExpirationDate = (EditText) findViewById(R.id.card_expiration_date);

        Braintree.setup(this,
                getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN),
                this);
    }

    @Override
    public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
            String errorMessage, Exception exception) {
        if (setupSuccessful) {
            mBraintree = braintree;
            mBraintree.addListener(this);

            Cart cart = getIntent().getParcelableExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART);
            boolean isBillingAgreement = getIntent().getBooleanExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT, false);
            boolean shippingAddressRequired = getIntent().getBooleanExtra("shippingAddressRequired", false);
            boolean phoneNumberRequired = getIntent().getBooleanExtra("phoneNumberRequired", false);
            mPaymentButton.setAndroidPayOptions(cart, isBillingAgreement, shippingAddressRequired,
                    phoneNumberRequired);
            boolean payPalAddressScopeRequested = getIntent().getBooleanExtra("payPalAddressScopeRequested", false);
            if (payPalAddressScopeRequested) {
                mPaymentButton.setAdditionalPayPalScopes(
                        Collections.singletonList(PayPal.SCOPE_ADDRESS));
            }
            mPaymentButton.initialize(this, mBraintree);

            findViewById(R.id.purchase_button).setEnabled(true);
        } else {
            Intent intent = new Intent()
                    .putExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE, errorMessage);
            setResult(RESULT_FIRST_USER, intent);
            finish();
        }
    }

    public void onPurchase(View v) {
        setProgressBarIndeterminateVisibility(true);

        CardBuilder cardBuilder = new CardBuilder()
            .cardNumber(mCardNumber.getText().toString())
            .expirationDate(mExpirationDate.getText().toString());

        mBraintree.tokenize(cardBuilder);
    }

    @Override
    public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD,
                        (Parcelable) paymentMethod));
        finish();
    }

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce));
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (responseCode == RESULT_OK && requestCode == PaymentButton.REQUEST_CODE) {
            setProgressBarIndeterminateVisibility(true);
            mBraintree.onActivityResult(this, requestCode, responseCode, data);
        } else {
            setProgressBarIndeterminateVisibility(false);
        }
    }
}
