package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.PayPalTwoFactorAuth;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PayPalTwoFactorAuthCallback;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalProductAttributes;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PayPalTwoFactorAuthRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.models.Nonce;
import com.braintreepayments.demo.models.PaymentMethodToken;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PayPalTwoFactorAuthActivity extends BaseActivity implements TextWatcher {

    private EditText mAmountEditText;
    private EditText mNonceEditText;
    private Button mBillingAgreementButton;
    private Button mPayPalTwoFactorAuthButton;

    private boolean billingAgreement = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paypal_two_factor_auth);

        mAmountEditText = findViewById(R.id.amount_edit_text);
        mAmountEditText.addTextChangedListener(this);

        mNonceEditText = findViewById(R.id.nonce_edit_text);
        mNonceEditText.addTextChangedListener(this);

        mBillingAgreementButton = findViewById(R.id.paypal_billing_agreement_button);
        mPayPalTwoFactorAuthButton = findViewById(R.id.paypal_two_factor_auth_button);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);
        if (billingAgreement) {

            final ApiClient apiClient = DemoApplication.getApiClient(this);

            // Use nonce from billing agreement to create a payment method token
            apiClient.createPaymentMethodToken(Settings.getCustomerId(this), paymentMethodNonce.getNonce(), new Callback<PaymentMethodToken>() {
                @Override
                public void success(PaymentMethodToken token, Response response) {

                    // When ready to transact, use payment method token to retrieve a new nonce
                    apiClient.createPaymentMethodNonce(token.getToken(), new Callback<Nonce>() {
                        @Override
                        public void success(Nonce nonce, Response response) {
                            mNonceEditText.setText(nonce.getNonce());
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            onError(error);
                        }
                    });
                }

                @Override
                public void failure(RetrofitError error) {
                    onError(error);
                }
            });

            billingAgreement = false;
        } else {
            Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            if (!Settings.SANDBOX_INDIA_ENV_NAME.equals(Settings.getEnvironment(this))) {
                onError(new Exception("To use feature, enable the \"Sandbox India\" environment."));
                return;
            }

            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
            mBillingAgreementButton.setEnabled(true);
        } catch (InvalidArgumentException e) {
            onError(e);
        }
    }

    @Override
    protected void reset() {
        mNonceEditText.setText("");
        mAmountEditText.setText(R.string.default_two_factor_amount);
        mBillingAgreementButton.setEnabled(false);
        mPayPalTwoFactorAuthButton.setEnabled(false);
    }

    public void launchBillingAgreement(View v) {
        billingAgreement = true;
        setProgressBarIndeterminateVisibility(true);

        PayPalProductAttributes attributes = new PayPalProductAttributes()
                .name("Braintree")
                .chargePattern("DEFERRED")
                .productCode("BT");

        PayPalRequest request = new PayPalRequest()
                .productAttributes(attributes)
                .localeCode("IN")
                .billingAgreementDescription("Braintree Test Payment");

        PayPal.requestBillingAgreement(mBraintreeFragment, request);
    }

    public void launchTwoFactorAuth(View v) {
        if (TextUtils.isEmpty(Settings.getCustomerId(this))) {
            onError(new Exception("Please add a customer ID in settings."));
            return;
        }

        PayPalTwoFactorAuthRequest request = new PayPalTwoFactorAuthRequest()
                .amount(mAmountEditText.getText().toString())
                .nonce(mNonceEditText.getText().toString())
                .currencyCode("INR"); // for now, our demo defaults to INR currency

        PayPalTwoFactorAuth.performTwoFactorLookup(mBraintreeFragment, request, new PayPalTwoFactorAuthCallback() {
            @Override
            public void onLookupResult(@NonNull PaymentMethodNonce nonce) {
                PayPalAccountNonce paypalAccountNonce = (PayPalAccountNonce)nonce;
                if (paypalAccountNonce.isTwoFactorAuthRequired()) {
                    PayPalTwoFactorAuth.continueTwoFactorAuthentication(mBraintreeFragment, paypalAccountNonce);
                } else {
                    // No authentication required; send nonce to server to transact
                }
            }

            @Override
            public void onLookupFailure(@NonNull Exception exception) {
                onError(exception);
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        mPayPalTwoFactorAuthButton.setEnabled(!TextUtils.isEmpty(mNonceEditText.getText()) &&
                !TextUtils.isEmpty(mAmountEditText.getText()));
    }
}
