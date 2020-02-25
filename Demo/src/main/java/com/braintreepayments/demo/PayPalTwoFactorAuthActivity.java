package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPalTwoFactorAuth;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PayPalTwoFactorAuthCallback;
import com.braintreepayments.api.models.PayPalTwoFactorAuthRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;

public class PayPalTwoFactorAuthActivity extends BaseActivity {

    private EditText mAmountEditText;
    private EditText mNonceEditText;
    private Button mPayPalTwoFactorAuthButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paypal_two_factor_auth);

        mAmountEditText = findViewById(R.id.amount_edit_text);
        mNonceEditText = findViewById(R.id.nonce_edit_text);
        mPayPalTwoFactorAuthButton = findViewById(R.id.paypal_two_factor_auth_button);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent()
                .putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
            enableButtons(true);
        } catch (InvalidArgumentException e) {
            onError(e);
        }
    }

    @Override
    protected void reset() {
        mNonceEditText.setText("");
        enableButtons(false);
    }

    private void enableButtons(boolean enabled) {
        mPayPalTwoFactorAuthButton.setEnabled(enabled);
    }

    public void onPayPalTwoFactorAuthButtonPress(View v) {
        PayPalTwoFactorAuthRequest request = new PayPalTwoFactorAuthRequest()
                .amount(mAmountEditText.getText().toString())
                .nonce(mNonceEditText.getText().toString())
                .currencyCode("INR"); // for now, our demo defaults to INR currency

        PayPalTwoFactorAuth.performTwoFactorLookup(mBraintreeFragment, request, new PayPalTwoFactorAuthCallback() {
            @Override
            public void onLookupResult(@NonNull PaymentMethodNonce nonce) {
                PayPalTwoFactorAuth.continueTwoFactorAuthentication(mBraintreeFragment, nonce);
            }

            @Override
            public void onLookupFailure(@NonNull Exception exception) {
                // notify error
                onError(exception);
            }
        });
    }
}
