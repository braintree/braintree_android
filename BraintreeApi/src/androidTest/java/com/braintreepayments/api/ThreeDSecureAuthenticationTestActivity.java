package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ThreeDSecureAuthenticationTestActivity extends Activity {

    public static final String EXTRA_CLIENT_TOKEN = "client_token";
    public static final String EXTRA_NONCE = "nonce";
    public static final String EXTRA_AMOUNT = "amount";

    private static final int THREE_D_SECURE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Braintree braintree = new Braintree(this, getIntent().getStringExtra(EXTRA_CLIENT_TOKEN));
        String nonce = getIntent().getStringExtra(EXTRA_NONCE);
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);

        braintree.startThreeDSecureVerification(this, THREE_D_SECURE_REQUEST, nonce, amount);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == THREE_D_SECURE_REQUEST) {
            setResult(resultCode, data);
            finish();
        }
    }

}
