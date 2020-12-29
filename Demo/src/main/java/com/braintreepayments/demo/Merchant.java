package com.braintreepayments.demo;

import android.content.Context;
import android.text.TextUtils;

import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;
import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.PayPalUAT;

import java.util.Locale;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Merchant {

    public void fetchPayPalUAT(final FetchPayPalUATCallback listener) {
        // NOTE: - The PP UAT is fetched from the PPCP sample server
        //       - The only feature that currently works with a PP UAT is Card Tokenization.
        ApiClient apiClient = new RestAdapter.Builder()
                .setEndpoint("https://ppcp-sample-merchant-sand.herokuapp.com")
                .setRequestInterceptor(new ApiClientRequestInterceptor())
                .build()
                .create(ApiClient.class);

        apiClient.getPayPalUAT("US", new Callback<PayPalUAT>() {
            @Override
            public void success(PayPalUAT uat, Response response) {
                String payPalUAT = uat.getUAT();
                if (TextUtils.isEmpty(payPalUAT)) {
                    listener.onResult(null, new Exception("PayPal UAT was empty"));
                } else {
                    listener.onResult(payPalUAT, null);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getResponse() == null) {
                    listener.onResult(null, error);
                } else {
                    int responseStatus = error.getResponse().getStatus();
                    String responseBody = error.getResponse().getBody().toString();

                    String errorFormat =
                        "Unable to get a PayPal UAT. Response Code: %d Response body: %s";
                    String errorMessage = String.format(
                            Locale.US, errorFormat, responseStatus, responseBody);

                    listener.onResult(null, new Exception(errorMessage));
                }
            }
        });
    }

    public void fetchClientToken(Context context, final FetchClientTokenCallback listener) {
        DemoApplication.getApiClient(context).getClientToken(Settings.getCustomerId(context),
                Settings.getMerchantAccountId(context), new Callback<ClientToken>() {
                    @Override
                    public void success(ClientToken clientToken, Response response) {
                        String token = clientToken.getClientToken();
                        if (TextUtils.isEmpty(token)) {
                            listener.onResult(null, new Exception("Client token was empty"));
                        } else {
                            listener.onResult(token, null);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {

                        if (error.getResponse() == null) {
                            listener.onResult(null, error);
                        } else {
                            int responseStatus = error.getResponse().getStatus();
                            String responseBody = error.getResponse().getBody().toString();

                            String errorFormat =
                                    "Unable to get a client token. Response Code: %d Response body: %s";
                            String errorMessage = String.format(
                                    Locale.US, errorFormat, responseStatus, responseBody);
                        }
                    }
                });
    }
}
