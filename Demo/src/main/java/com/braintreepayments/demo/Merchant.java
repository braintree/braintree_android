package com.braintreepayments.demo;

import android.content.Context;
import android.text.TextUtils;

import com.braintreepayments.demo.models.ClientToken;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Merchant {

    public void fetchClientToken(Context context, final FetchClientTokenCallback listener) {

        String customerId = Settings.getCustomerId(context);
        String merchantAccountId = Settings.getMerchantAccountId(context);

        DemoApplication
                .getApiClient(context)
                .getClientToken(customerId, merchantAccountId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<ClientToken> call, Response<ClientToken> response) {
                        if (response.isSuccessful()) {
                            String token = response.body().getClientToken();
                            if (TextUtils.isEmpty(token)) {
                                listener.onResult(new FetchClientTokenResult.Error(
                                        new Exception("Client token was empty")
                                ));
                            } else {
                                listener.onResult(new FetchClientTokenResult.Success(token));
                            }
                        } else {
                            String responseBody;
                            if (response.body() != null) {
                                responseBody = response.body().toString();
                            } else {
                                responseBody = "empty response body";
                            }

                            String errorFormat =
                                    "Unable to get a client token. Response Code: %d Response body: %s";
                            String errorMessage = String.format(
                                    Locale.US, errorFormat, response.code(), responseBody);

                            listener.onResult(new FetchClientTokenResult.Error(
                                    new Exception(errorMessage))
                            );
                        }
                    }

                    @Override
                    public void onFailure(Call<ClientToken> call, Throwable throwable) {
                        listener.onResult(new FetchClientTokenResult.Error(new Exception(throwable)));
                    }
                });
    }
}
