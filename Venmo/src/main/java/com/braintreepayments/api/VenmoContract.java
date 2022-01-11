package com.braintreepayments.api;

import static com.braintreepayments.api.VenmoClient.APP_SWITCH_ACTIVITY;
import static com.braintreepayments.api.VenmoClient.EXTRA_ACCESS_TOKEN;
import static com.braintreepayments.api.VenmoClient.EXTRA_BRAINTREE_DATA;
import static com.braintreepayments.api.VenmoClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.VenmoClient.EXTRA_MERCHANT_ID;
import static com.braintreepayments.api.VenmoClient.EXTRA_PAYMENT_METHOD_NONCE;
import static com.braintreepayments.api.VenmoClient.EXTRA_RESOURCE_ID;
import static com.braintreepayments.api.VenmoClient.EXTRA_USERNAME;
import static com.braintreepayments.api.VenmoClient.META_KEY;
import static com.braintreepayments.api.VenmoClient.VENMO_PACKAGE_NAME;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class VenmoContract extends ActivityResultContract<VenmoContractInput, VenmoResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, VenmoContractInput input) {
        ComponentName venmoComponentName =
                new ComponentName(VENMO_PACKAGE_NAME, VENMO_PACKAGE_NAME + "." + APP_SWITCH_ACTIVITY);

        Intent venmoIntent = new Intent()
                .setComponent(venmoComponentName)
                .putExtra(EXTRA_MERCHANT_ID, input.getProfileId())
                .putExtra(EXTRA_ACCESS_TOKEN, input.getVenmoAccessToken())
                .putExtra(EXTRA_ENVIRONMENT, input.getVenmoEnvironment());

        String paymentContextId = input.getPaymentContextId();
        if (paymentContextId != null) {
            venmoIntent.putExtra(EXTRA_RESOURCE_ID, paymentContextId);
        }

        try {
            JSONObject braintreeData = new JSONObject();

            JSONObject meta = new MetadataBuilder()
                    .sessionId(input.getBraintreeSessionId())
                    .integration(input.getBraintreeIntegrationType())
                    .version()
                    .build();

            braintreeData.put(META_KEY, meta);
            venmoIntent.putExtra(EXTRA_BRAINTREE_DATA, braintreeData.toString());
        } catch (JSONException ignored) {
        }

        return venmoIntent;
    }

    @Override
    public VenmoResult parseResult(int resultCode, @Nullable Intent intent) {
        if (intent != null) {
            String paymentContextId = null;
            String venmoAccountNonce = null;
            String venmoUsername = null;
            Exception error = null;
            if (resultCode == AppCompatActivity.RESULT_OK) {
                paymentContextId = intent.getStringExtra(EXTRA_RESOURCE_ID);
                if (paymentContextId == null) {
                    venmoAccountNonce = intent.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);
                    venmoUsername = intent.getStringExtra(EXTRA_USERNAME);
                }
            } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
                error = new UserCanceledException("User canceled Venmo.");
            }
            return new VenmoResult(paymentContextId, venmoAccountNonce, venmoUsername, error);
        }
        return null;
    }
}
