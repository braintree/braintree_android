package com.braintreepayments.demo;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.braintreepayments.FetchAuthorizationCallback;
import com.braintreepayments.api.BraintreeAuthCallback;
import com.braintreepayments.api.BraintreeAuthProvider;

public class DemoAuthorizationProvider implements BraintreeAuthProvider {

    private final Context appContext;
    private final Merchant merchant;

    public DemoAuthorizationProvider(Context context) {
        appContext = context.getApplicationContext();
        merchant = new Merchant();
    }

    public void fetchAuthorization(Context context, final FetchAuthorizationCallback callback) {
        String authType = Settings.getAuthorizationType(context);
        if (authType.equals(getString(context, R.string.tokenization_key))) {
            callback.onResult(Settings.getTokenizationKey(context), null);
        } else if (authType.equals(getString(context, R.string.paypal_uat))) {
            // NOTE: - The PP UAT is fetched from the PPCP sample server
            //       - The only feature that currently works with a PP UAT is Card Tokenization.
            merchant.fetchPayPalUAT(new FetchPayPalUATCallback() {
                @Override
                public void onResult(@Nullable String payPalUAT, @Nullable Exception error) {
                    callback.onResult(payPalUAT, error);
                }
            });

        } else if (authType.equals(getString(context, R.string.client_token))) {
            merchant.fetchClientToken(context, new FetchClientTokenCallback() {
                @Override
                public void onResult(@Nullable String clientToken, @Nullable Exception error) {
                    callback.onResult(clientToken, error);
                }
            });
        }
    }

    private static String getString(Context context, @StringRes int id) {
        return context.getResources().getString(id);
    }

    @Override
    public void getAuthorization(BraintreeAuthCallback callback) {
        String authType = Settings.getAuthorizationType(appContext);
        if (authType.equals(getString(appContext, R.string.tokenization_key))) {
            callback.onAuthResult(Settings.getTokenizationKey(appContext), null);
        } else if (authType.equals(getString(appContext, R.string.paypal_uat))) {
            // NOTE: - The PP UAT is fetched from the PPCP sample server
            //       - The only feature that currently works with a PP UAT is Card Tokenization.
            merchant.fetchPayPalUAT(new FetchPayPalUATCallback() {
                @Override
                public void onResult(@Nullable String payPalUAT, @Nullable Exception error) {
                    callback.onAuthResult(payPalUAT, error);
                }
            });

        } else if (authType.equals(getString(appContext, R.string.client_token))) {
            merchant.fetchClientToken(appContext, new FetchClientTokenCallback() {
                @Override
                public void onResult(@Nullable String clientToken, @Nullable Exception error) {
                    callback.onAuthResult(clientToken, error);
                }
            });
        }
    }
}
