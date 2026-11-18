package com.braintreepayments.demo;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.braintreepayments.demo.models.ClientToken;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Merchant {

    public void fetchClientToken(Context context, final FetchClientTokenCallback listener) {
        fetchClientToken(context, null, listener);
    }

    /**
     * Fetches a client token. A {@code preferredPaymentMethodToken} is sent as {@code preferred_payment_method_token},
     * so the client token carries that payment method's {@code paymentMethodIdJwt}.
     */
    public void fetchClientToken(
            Context context,
            @Nullable String preferredPaymentMethodToken,
            final FetchClientTokenCallback listener
    ) {

        String customerId = Settings.getCustomerId(context);
        String merchantAccountId = Settings.getMerchantAccountId(context);

        DemoApplication
                .getApiClient(context)
                .getClientToken(customerId, merchantAccountId, preferredPaymentMethodToken)
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
