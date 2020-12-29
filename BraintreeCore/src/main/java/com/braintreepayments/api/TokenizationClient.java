package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.GraphQLConstants.Features;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;

import static com.braintreepayments.api.models.PaymentMethodNonce.parsePaymentMethodNonces;

public class TokenizationClient {

    static final String PAYMENT_METHOD_ENDPOINT = "payment_methods";

    private final BraintreeClient braintreeClient;

    TokenizationClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Create a {@link PaymentMethodNonce} in the Braintree Gateway.
     * <p>
     * On completion, returns the {@link PaymentMethodNonce} to {@link PaymentMethodNonceCallback}.
     * <p>
     * If creation fails validation, {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     * will be called with the resulting {@link ErrorWithResponse}.
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)} (Throwable)}
     * will be called with the {@link Exception} that occurred.
     *
     * @param paymentMethodBuilder {@link PaymentMethodBuilder} for the {@link PaymentMethodNonce}
     *        to be created.
     */
    public void tokenize(final Context context, final PaymentMethodBuilder paymentMethodBuilder, final PaymentMethodNonceCallback callback) {
        paymentMethodBuilder.setSessionId(braintreeClient.getSessionId());

        braintreeClient.getConfiguration(context, new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    if (paymentMethodBuilder instanceof CardBuilder &&
                            configuration.getGraphQL().isFeatureEnabled(Features.TOKENIZE_CREDIT_CARDS)) {
                        tokenizeGraphQL(context, (CardBuilder) paymentMethodBuilder, callback);
                    } else {
                        tokenizeRest(context, paymentMethodBuilder, callback);
                    }
                } else {
                    callback.failure(error);
                }
            }
        });
    }

    private void tokenizeGraphQL(final Context context, final CardBuilder cardBuilder, final PaymentMethodNonceCallback callback) {
        braintreeClient.sendAnalyticsEvent(context, "card.graphql.tokenization.started");
        final String payload;
        try {
            payload = cardBuilder.buildGraphQL(context, braintreeClient.getAuthorization());
        } catch (BraintreeException e) {
            callback.failure(e);
            return;
        }

        braintreeClient.sendGraphQLPOST(payload, context, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    callback.success(parsePaymentMethodNonces(responseBody, cardBuilder.getResponsePaymentMethodType()));
                    braintreeClient.sendAnalyticsEvent(context, "card.graphql.tokenization.success");
                } catch (JSONException e) {
                    callback.failure(e);
                }
            }

            @Override
            public void failure(Exception exception) {
                braintreeClient.sendAnalyticsEvent(context, "card.graphql.tokenization.failure");
                callback.failure(exception);
            }
        });
    }

    private void tokenizeRest(Context context, final PaymentMethodBuilder paymentMethodBuilder, final PaymentMethodNonceCallback callback) {
        String url = TokenizationClient.versionedPath(
                TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + paymentMethodBuilder.getApiPath());

        braintreeClient.sendPOST(url, paymentMethodBuilder.build(), context, new HttpResponseCallback() {

            @Override
            public void success(String responseBody) {
                try {
                    callback.success(parsePaymentMethodNonces(responseBody,
                            paymentMethodBuilder.getResponsePaymentMethodType()));
                } catch (JSONException e) {
                    callback.failure(e);
                }
            }

            @Override
            public void failure(Exception exception) {
                callback.failure(exception);
            }
        });
    }

    static String versionedPath(String path) {
        return "/v1/" + path;
    }
}
