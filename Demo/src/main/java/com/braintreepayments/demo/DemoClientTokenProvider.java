package com.braintreepayments.demo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.braintreepayments.api.core.BraintreeClient;

public class DemoClientTokenProvider  {

    private final Merchant merchant;
    private final Context appContext;

    public DemoClientTokenProvider(Context context) {
        merchant = new Merchant();
        appContext = context.getApplicationContext();
    }

    public void getClientToken(@NonNull BraintreeAuthorizationCallback callback) {
        String authType = Settings.getAuthorizationType(appContext);
        if (authType.equals(getString(appContext, R.string.client_token))) {
            merchant.fetchClientToken(appContext, (clientToken, error) -> {
                if (clientToken != null) {
                    callback.onResult(clientToken);
                } else if (error != null) {
                    callback.onResult(null);
                }
            });
        } else {
            String key;
            if (Settings.showCheckoutExperience(appContext)) {
                key = Settings.getPayPalCheckoutTokenizationKey(appContext);
            } else {
                key = Settings.getTokenizationKey(appContext);
            }
            callback.onResult(key);
        }
    }

    private static String getString(Context context, @StringRes int id) {
        return context.getResources().getString(id);
    }
}
