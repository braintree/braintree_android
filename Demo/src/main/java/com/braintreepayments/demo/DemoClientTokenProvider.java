package com.braintreepayments.demo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.braintreepayments.api.ClientTokenCallback;
import com.braintreepayments.api.ClientTokenProvider;

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
        if (authType.equals(getString(appContext, R.string.paypal_uat))) {
            // NOTE: - The PP UAT is fetched from the PPCP sample server
            //       - The only feature that currently works with a PP UAT is Card Tokenization.
            merchant.fetchPayPalUAT((payPalUAT, error) -> {
                if (payPalUAT != null) {
                    callback.onSuccess(payPalUAT);
                } else if (error != null) {
                    callback.onFailure(error);
                }
            });

        } else if (authType.equals(getString(appContext, R.string.client_token))) {
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
