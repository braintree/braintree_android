package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.GraphQLConstants.Features;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

class TokenizationClient {

    static final String PAYMENT_METHOD_ENDPOINT = "payment_methods";

    private final WeakReference<BraintreeClient> braintreeClientRef;

    TokenizationClient(BraintreeClient braintreeClient) {
        this(new WeakReference<>(braintreeClient));
    }

    @VisibleForTesting
    TokenizationClient(WeakReference<BraintreeClient> braintreeClientRef) {
        this.braintreeClientRef = braintreeClientRef;
    }

    /**
     * Create a {@link PaymentMethodNonce} in the Braintree Gateway.
     * <p>
     * On completion, returns the {@link PaymentMethodNonce} to {@link TokenizeCallback}.
     * <p>
     * If creation fails validation, {@link TokenizeCallback#onResult(org.json.JSONObject, Exception)}
     * will be called with the resulting {@link ErrorWithResponse}.
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * TokenizeCallback#onResult(org.json.JSONObject, Exception)} will be called with the {@link Exception} that occurred.
     *
     * @param paymentMethod {@link PaymentMethod} for the {@link PaymentMethodNonce}
     *        to be created.
     * @param callback {@link TokenizeCallback}
     */
    void tokenize(final PaymentMethod paymentMethod, final TokenizeCallback callback) {
        final BraintreeClient braintreeClient = braintreeClientRef.get();
        if (braintreeClient == null) {
            return;
        }

        paymentMethod.setSessionId(braintreeClient.getSessionId());
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    if (paymentMethod instanceof GraphQLTokenizable &&
                            configuration.isGraphQLFeatureEnabled(Features.TOKENIZE_CREDIT_CARDS)) {
                        tokenizeGraphQL(braintreeClient, (GraphQLTokenizable) paymentMethod, callback);
                    } else {
                        tokenizeRest(braintreeClient, paymentMethod, callback);
                    }
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    private static void tokenizeGraphQL(final BraintreeClient braintreeClient, final GraphQLTokenizable graphQLTokenizable, final TokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.started");
        final JSONObject payload;
        try {
            payload = graphQLTokenizable.buildGraphQLTokenizationJSON();
        } catch (BraintreeException e) {
            callback.onResult(null, e);
            return;
        }

        braintreeClient.sendGraphQLPOST(payload.toString(), new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    callback.onResult(new JSONObject(responseBody), null);
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.success");
                } catch (JSONException exception) {
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure");
                    callback.onResult(null, exception);
                }
            }

            @Override
            public void failure(Exception exception) {
                braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure");
                callback.onResult(null, exception);
            }
        });
    }

    private static void tokenizeRest(final BraintreeClient braintreeClient, final PaymentMethod paymentMethod, final TokenizeCallback callback) {
        String url = TokenizationClient.versionedPath(
                TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + paymentMethod.getApiPath());

        braintreeClient.sendPOST(url, paymentMethod.buildTokenizationJSON().toString(), new HttpResponseCallback() {

            @Override
            public void success(String responseBody) {
                try {
                    callback.onResult(new JSONObject(responseBody), null);
                } catch (JSONException exception) {
                    callback.onResult(null, exception);
                }
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, exception);
            }
        });
    }

    static String versionedPath(String path) {
        return "/v1/" + path;
    }
}
