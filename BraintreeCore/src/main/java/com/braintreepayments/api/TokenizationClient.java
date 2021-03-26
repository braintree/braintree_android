package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.GraphQLConstants.Features;

import org.json.JSONException;

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
     * If creation fails validation, {@link TokenizeCallback#failure(Exception)}
     * will be called with the resulting {@link ErrorWithResponse}.
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * TokenizeCallback#failure(Exception)} will be called with the {@link Exception} that occurred.
     *
     * @param paymentMethodBuilder {@link PaymentMethodBuilder} for the {@link PaymentMethodNonce}
     *        to be created.
     * @param callback {@link TokenizeCallback}
     */
    <T> void tokenize(final PaymentMethodBuilder<T> paymentMethodBuilder, final TokenizeCallback callback) {
        final BraintreeClient braintreeClient = braintreeClientRef.get();
        if (braintreeClient == null) {
            return;
        }

        paymentMethodBuilder.setSessionId(braintreeClient.getSessionId());
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    if (paymentMethodBuilder instanceof CardBuilder &&
                            configuration.isGraphQLFeatureEnabled(Features.TOKENIZE_CREDIT_CARDS)) {
                        tokenizeGraphQL(braintreeClient, (CardBuilder) paymentMethodBuilder, callback);
                    } else {
                        tokenizeRest(braintreeClient, paymentMethodBuilder, callback);
                    }
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    private static void tokenizeGraphQL(final BraintreeClient braintreeClient, final CardBuilder cardBuilder, final TokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.started");
        final String payload;
        try {
            payload = cardBuilder.buildGraphQL(braintreeClient.getAuthorization());
        } catch (BraintreeException e) {
            callback.onResult(null, e);
            return;
        }

        braintreeClient.sendGraphQLPOST(payload, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    callback.onResult(TokenizationResult.fromJson(responseBody), null);
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.success");
                } catch (JSONException e) {
                    callback.onResult(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure");
                callback.onResult(null, e);
            }
        });
    }

    private static <T> void tokenizeRest(final BraintreeClient braintreeClient, final PaymentMethodBuilder<T> paymentMethodBuilder, final TokenizeCallback callback) {
        String url = TokenizationClient.versionedPath(
                TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + paymentMethodBuilder.getApiPath());

        braintreeClient.sendPOST(url, paymentMethodBuilder.build(), new HttpResponseCallback() {

            @Override
            public void success(String responseBody) {
                try {
                    callback.onResult(TokenizationResult.fromJson(responseBody), null);
                } catch (JSONException e) {
                    callback.onResult(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                callback.onResult(null, e);
            }
        });
    }

    static String versionedPath(String path) {
        return "/v1/" + path;
    }
}
