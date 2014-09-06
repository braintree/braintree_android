package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import static android.app.Activity.RESULT_OK;

public class VenmoAppSwitch extends AppSwitch {

    public VenmoAppSwitch(Context context, ClientToken clientToken) {
        super(context, clientToken);
    }

    @Override
    protected boolean isAvailable() {
        return super.isAvailable() && !mClientToken.getVenmoState().equals("off");
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
        Intent intent = super.getLaunchIntent().putExtra(EXTRA_MERCHANT_ID, mClientToken.getMerchantId());
        if (mClientToken.getVenmoState().equals("offline")) {
            intent.putExtra(EXTRA_OFFLINE, true);
        } else if (mClientToken.getVenmoState().equals("live")) {
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
