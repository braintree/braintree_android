package com.braintreepayments.demo;

import android.content.Context;
import android.text.TextUtils;

import com.braintreepayments.demo.models.ClientToken;

import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Merchant {

    public void fetchClientToken(Context context, final FetchClientTokenCallback listener) {

        String customerId = Settings.getCustomerId(context);
        String merchantAccountId = Settings.getMerchantAccountId(context);

        ClientTokenRequest request = new ClientTokenRequest(customerId, merchantAccountId);
        DemoApplication.getApiClient(context).getClientToken(request, new Callback<ClientToken>() {
            @Override
            public void success(ClientToken clientToken, Response response) {
                String token = clientToken.getValue();
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

                    listener.onResult(null, new Exception(errorMessage));
                }
            }
        });
    }
}
