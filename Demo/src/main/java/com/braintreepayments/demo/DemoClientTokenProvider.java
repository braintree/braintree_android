package com.braintreepayments.demo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class DemoClientTokenProvider {

    private final Merchant merchant;
    private final Context appContext;

    public DemoClientTokenProvider(Context context) {
        merchant = new Merchant();
        appContext = context.getApplicationContext();
    }

    public void getClientToken(@NonNull BraintreeAuthorizationCallback callback) {
        String authType = Settings.getAuthorizationType(appContext);
        if (authType.equals(getString(appContext, R.string.client_token))) {
            merchant.fetchClientToken(appContext, (result) -> {
                if (result instanceof FetchClientTokenResult.Success) {
                    callback.onResult(new BraintreeAuthorizationResult.Success(
                        ((FetchClientTokenResult.Success) result).getClientToken())
                    );
                } else if (result instanceof FetchClientTokenResult.Error) {
                    callback.onResult(new BraintreeAuthorizationResult.Error(
                        ((FetchClientTokenResult.Error) result).getError())
                    );
                }
            });
        } else {
            String key;
            if (Settings.showCheckoutExperience(appContext)) {
                key = Settings.getPayPalCheckoutTokenizationKey(appContext);
            } else {
                key = Settings.getTokenizationKey(appContext);
            }
            if (key != null) {
                callback.onResult(new BraintreeAuthorizationResult.Success(key));
            } else {
                callback.onResult(new BraintreeAuthorizationResult.Error(
                    new IllegalArgumentException("No tokenization key available")
                ));
            }
        }
    }

    private static String getString(Context context, @StringRes int id) {
        return context.getResources().getString(id);
    }
}
