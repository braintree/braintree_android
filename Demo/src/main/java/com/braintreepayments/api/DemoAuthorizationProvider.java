package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.braintreepayments.demo.Merchant;
import com.braintreepayments.demo.R;
import com.braintreepayments.demo.Settings;

// TODO: move back to com.braintreepayments.demo when AuthorizationProvider is released
public class DemoAuthorizationProvider implements AuthorizationProvider {

    private final Merchant merchant;
    private final Context appContext;

    public DemoAuthorizationProvider(Context context) {
        merchant = new Merchant();
        appContext = context.getApplicationContext();
    }

    @Override
    public void getClientToken(@NonNull ClientTokenCallback callback) {
        String authType = Settings.getAuthorizationType(appContext);
        if (authType.equals(getString(appContext, R.string.client_token))) {
            merchant.fetchClientToken(appContext, (clientToken, error) -> {
                if (clientToken != null) {
                    callback.onSuccess(clientToken);
                } else if (error != null) {
                    callback.onFailure(error);
                }
            });
        }
    }

    private static String getString(Context context, @StringRes int id) {
        return context.getResources().getString(id);
    }
}
