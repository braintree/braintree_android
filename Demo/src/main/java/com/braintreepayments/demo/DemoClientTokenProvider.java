package com.braintreepayments.demo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.braintreepayments.api.AuthorizationProvider;
import com.braintreepayments.api.ClientTokenCallback;
import com.braintreepayments.demo.Merchant;
import com.braintreepayments.demo.R;
import com.braintreepayments.demo.Settings;

public class DemoClientTokenProvider implements ClientTokenProvider {

    private final Merchant merchant;
    private final Context appContext;

    public DemoClientTokenProvider(Context context) {
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
