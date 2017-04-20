package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountNonce;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class VenmoActivity extends BaseActivity implements ConfigurationListener {

    private ImageButton mVenmoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.venmo_activity);
        setUpAsBack();

        mVenmoButton = (ImageButton) findViewById(R.id.venmo_button);
    }

    @Override
    protected void reset() {
        mVenmoButton.setVisibility(GONE);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
        } catch (InvalidArgumentException e) {
            onError(e);
        }
    }

    @Override
    public void onConfigurationFetched(Configuration configuration) {
        if (configuration.getPayWithVenmo().isEnabled(this)) {
            mVenmoButton.setVisibility(VISIBLE);
        } else if (configuration.getPayWithVenmo().isAccessTokenValid()) {
            showDialog("Please install the Venmo app first.");
        } else {
            showDialog("Venmo is not enabled for the current merchant.");
        }
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce);

        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void launchVenmo(View v) {
        setProgressBarIndeterminateVisibility(true);

        Venmo.authorizeAccount(mBraintreeFragment, Settings.vaultVenmo(this) && !TextUtils.isEmpty(Settings.getCustomerId(this)));
    }

    public static String getDisplayString(VenmoAccountNonce nonce) {
        return "Username: " + nonce.getUsername();
    }
}
