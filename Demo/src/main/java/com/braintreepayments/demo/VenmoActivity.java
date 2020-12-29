package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.VenmoAuthorizeAccountCallback;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountNonce;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class VenmoActivity extends BaseActivity {

    private ImageButton mVenmoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.venmo_activity);
        setUpAsBack();

        mVenmoButton = findViewById(R.id.venmo_button);

        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (isVenmoAppSwitchEnabled()) {
                    mVenmoButton.setVisibility(VISIBLE);
                } else if (configuration.getPayWithVenmo().isAccessTokenValid()) {
                    showDialog("Please install the Venmo app first.");
                } else {
                    showDialog("Venmo is not enabled for the current merchant.");
                }
            }
        });
    }

    @Override
    protected void reset() {
        mVenmoButton.setVisibility(GONE);
    }

    @Override
    protected void onVenmoResult(PaymentMethodNonce paymentMethodNonce, Exception error) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void launchVenmo(View v) {
        setProgressBarIndeterminateVisibility(true);

        boolean shouldVault =
            Settings.vaultVenmo(this) && !TextUtils.isEmpty(Settings.getCustomerId(this));

        authorizeVenmoAccount(shouldVault, null, new VenmoAuthorizeAccountCallback() {
            @Override
            public void onResult(boolean isAuthorized, Exception error) {
                if (error != null) {
                    onBraintreeError(error);
                }
            }
        });
    }

    public static String getDisplayString(VenmoAccountNonce nonce) {
        return "Username: " + nonce.getUsername();
    }
}
