package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.VenmoAuthorizeAccountCallback;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoOnActivityResultCallback;
import com.braintreepayments.api.InvalidArgumentException;
import com.braintreepayments.api.Authorization;
import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.VenmoAccountNonce;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class VenmoActivity extends BaseActivity {

    private ImageButton mVenmoButton;
    private VenmoClient venmoClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.venmo_activity);
        setUpAsBack();

        mVenmoButton = findViewById(R.id.venmo_button);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            Authorization authorization = Authorization.fromString(mAuthorization);
            BraintreeClient braintreeClient = new BraintreeClient(authorization, this, RETURN_URL_SCHEME);
            venmoClient = new VenmoClient(braintreeClient);

            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                    if (venmoClient.isVenmoAppSwitchAvailable(VenmoActivity.this)) {
                        mVenmoButton.setVisibility(VISIBLE);
                    } else if (configuration.getPayWithVenmo().isAccessTokenValid()) {
                        showDialog("Please install the Venmo app first.");
                    } else {
                        showDialog("Venmo is not enabled for the current merchant.");
                    }
                }
            });
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void reset() {
        mVenmoButton.setVisibility(GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        venmoClient.onActivityResult(this, resultCode, data, new VenmoOnActivityResultCallback() {
            @Override
            public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                handleVenmoResult(venmoAccountNonce);
            }
        });
    }

    private void handleVenmoResult(VenmoAccountNonce venmoAccountNonce) {
        super.onPaymentMethodNonceCreated(venmoAccountNonce);

        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_RESULT, venmoAccountNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void launchVenmo(View v) {
        setProgressBarIndeterminateVisibility(true);

        boolean shouldVault =
            Settings.vaultVenmo(this) && !TextUtils.isEmpty(Settings.getCustomerId(this));

        venmoClient.authorizeAccount(this, shouldVault, null, new VenmoAuthorizeAccountCallback() {
            @Override
            public void onResult(Exception error) {
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
