package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

class VenmoActivityResultContract extends ActivityResultContract<VenmoIntentData, VenmoResult> {

    static final String VENMO_PACKAGE_NAME = "com.venmo";
    static final String APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    static final String META_KEY = "_meta";
    static final String EXTRA_MERCHANT_ID = "com.braintreepayments.api.MERCHANT_ID";
    static final String EXTRA_ACCESS_TOKEN = "com.braintreepayments.api.ACCESS_TOKEN";
    static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.ENVIRONMENT";
    static final String EXTRA_BRAINTREE_DATA = "com.braintreepayments.api.EXTRA_BRAINTREE_DATA";
    static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
    static final String EXTRA_USERNAME = "com.braintreepayments.api.EXTRA_USER_NAME";
    static final String EXTRA_RESOURCE_ID = "com.braintreepayments.api.EXTRA_RESOURCE_ID";

    @VisibleForTesting
    boolean shouldVault;

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, VenmoIntentData input) {
        Intent venmoIntent = getVenmoIntent()
                .putExtra(EXTRA_MERCHANT_ID, input.getProfileId())
                .putExtra(EXTRA_ACCESS_TOKEN, input.getConfiguration().getVenmoAccessToken())
                .putExtra(EXTRA_ENVIRONMENT, input.getConfiguration().getVenmoEnvironment());

        if (input.getPaymentContextId() != null) {
            venmoIntent.putExtra(EXTRA_RESOURCE_ID, input.getPaymentContextId());
        }

        Uri encodedVenmoURL = Uri.parse("www.paypal.com");
        try {
            JSONObject braintreeData = new JSONObject();

            JSONObject meta = new MetadataBuilder()
                    .sessionId(input.getSessionId())
                    .integration(input.getIntegrationType())
                    .version()
                    .build();

            braintreeData.put(META_KEY, meta);
            venmoIntent.putExtra(EXTRA_BRAINTREE_DATA, braintreeData.toString());

            // TODO: update return URL, switch between base URLs
            Uri venmoBaseURL = Uri.parse("https://venmo.com/go/checkout");
            encodedVenmoURL = venmoBaseURL.buildUpon()
                    .appendQueryParameter("x-success", "com.braintreepayments.demo://x-callback-url/vzero/auth/venmo/success")
                    .appendQueryParameter("x-error", "com.braintreepayments.demo://x-callback-url/vzero/auth/venmo/error")
                    .appendQueryParameter("x-cancel", "com.braintreepayments.demo://x-callback-url/vzero/auth/venmo/cancel")
                    .appendQueryParameter("x-source", "Demo")
                    .appendQueryParameter("braintree_merchant_id", input.getProfileId())
                    .appendQueryParameter("braintree_access_token", input.getConfiguration().getVenmoAccessToken())
                    .appendQueryParameter("braintree_environment", input.getConfiguration().getVenmoEnvironment())
                    .appendQueryParameter("resource_id", input.getPaymentContextId())
                    .appendQueryParameter("braintree_sdk_data", braintreeData.toString())
                    .appendQueryParameter("customerClient", "MOBILE_APP")
                    .build();
        } catch (JSONException ignored) {
            /* do nothing */
        }

        // TODO: update this logic
//        return venmoIntent;
        return new Intent(Intent.ACTION_VIEW, encodedVenmoURL);
    }

    @Override
    public VenmoResult parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (intent == null) {
                return new VenmoResult(null, null, null, new BraintreeException("An unknown Android error occurred with the activity result API."));
            }
            String paymentContextId = intent.getStringExtra(EXTRA_RESOURCE_ID);
            String nonce = intent.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);
            String venmoUsername = intent.getStringExtra(EXTRA_USERNAME);
            return new VenmoResult(paymentContextId, nonce, venmoUsername, null);
        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            return new VenmoResult(null, null, null, new UserCanceledException("User canceled Venmo."));
        }
        return null;
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(VENMO_PACKAGE_NAME, VENMO_PACKAGE_NAME + "." + APP_SWITCH_ACTIVITY));
    }
}
