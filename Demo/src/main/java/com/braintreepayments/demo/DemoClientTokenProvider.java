package com.braintreepayments.demo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

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
            callback.onResult(Settings.getTokenizationKey(appContext));
        }
    }

    private static String getString(Context context, @StringRes int id) {
        return context.getResources().getString(id);
    }
}
