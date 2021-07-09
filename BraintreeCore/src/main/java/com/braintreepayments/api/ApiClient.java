package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

class ApiClient {

    static final String PAYMENT_METHOD_ENDPOINT = "payment_methods";

    private final WeakReference<BraintreeClient> braintreeClientRef;

    ApiClient(BraintreeClient braintreeClient) {
        this(new WeakReference<>(braintreeClient));
    }

    @VisibleForTesting
    ApiClient(WeakReference<BraintreeClient> braintreeClientRef) {
        this.braintreeClientRef = braintreeClientRef;
    }

    void tokenizeGraphQL(final JSONObject tokenizePayload, final TokenizeCallback callback) {
        final BraintreeClient braintreeClient = braintreeClientRef.get();
        if (braintreeClient == null) {
            return;
        }

        braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.started");
        braintreeClient.sendGraphQLPOST(tokenizePayload.toString(), new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                if (responseBody != null) {
                    try {
                        callback.onResult(new JSONObject(responseBody), null);
                        braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.success");
                    } catch (JSONException exception) {
                        braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure");
                        callback.onResult(null, exception);
                    }
                } else {
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure");
                    callback.onResult(null, httpError);
                }
            }
        });
    }

    void tokenizeREST(final PaymentMethod paymentMethod, final TokenizeCallback callback) {
        final BraintreeClient braintreeClient = braintreeClientRef.get();
        if (braintreeClient == null) {
            return;
        }

        String url = ApiClient.versionedPath(
                ApiClient.PAYMENT_METHOD_ENDPOINT + "/" + paymentMethod.getApiPath());

        paymentMethod.setSessionId(braintreeClient.getSessionId());

        try {
            braintreeClient.sendPOST(url, paymentMethod.buildJSON().toString(), new HttpResponseCallback() {

                @Override
                public void onResult(String responseBody, Exception httpError) {
                    if (responseBody != null) {
                        try {
                            callback.onResult(new JSONObject(responseBody), null);
                        } catch (JSONException exception) {
                            callback.onResult(null, exception);
                        }
                    } else {
                        callback.onResult(null, httpError);
                    }
                }
            });
        } catch (JSONException exception) {
            callback.onResult(null, exception);
        }
    }

    static String versionedPath(String path) {
        return "/v1/" + path;
    }
}
