package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import com.braintreepayments.api.models.Configuration;

import static android.app.Activity.RESULT_OK;

public class VenmoAppSwitch extends AppSwitch {

    public static final String VENMO_SOURCE = "venmo-app";

    public VenmoAppSwitch(Context context, Configuration configuration) {
        super(context, configuration);
    }

    /**
     * Checks the given {@link Intent} to see if it contains a {@link AppSwitch#EXTRA_PAYMENT_METHOD_NONCE}.
     *
     * @param intent The {@link Intent} to check.
     * @return {@code true} is the {@link Intent} contains a {@link AppSwitch#EXTRA_PAYMENT_METHOD_NONCE},
     *         {@code false} otherwise.
     */
    public static boolean isVenmoAppSwitchResponse(Intent intent) {
        return intent.hasExtra(EXTRA_PAYMENT_METHOD_NONCE);
    }

    @Override
    protected boolean isAvailable() {
        return super.isAvailable() && !mConfiguration.getVenmoState().equals("off");
    }

    @Override
    protected String getPackage() {
        return "com.venmo";
    }

    @Override
    protected String getAppSwitchActivity() {
        return "CardChooserActivity";
    }

    @Override
    protected String getCertificateSubject() {
        return "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    }

    @Override
    protected String getCertificateIssuer() {
        return "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    }

    @Override
    protected int getPublicKeyHashCode() {
        return -129711843;
    }

    @Override
    protected Intent getLaunchIntent() {
        Intent intent = super.getLaunchIntent().putExtra(EXTRA_MERCHANT_ID, mConfiguration.getMerchantId());
        if (mConfiguration.getVenmoState().equals("offline")) {
            intent.putExtra(EXTRA_OFFLINE, true);
        } else if (mConfiguration.getVenmoState().equals("live")) {
            intent.putExtra(EXTRA_OFFLINE, false);
        }
        return intent;
    }

    @Override
    protected String handleAppSwitchResponse(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            return data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);
        }
        return null;
    }

}
