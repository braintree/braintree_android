package com.braintreepayments.api;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.GraphQLConstants.Features;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;

import static com.braintreepayments.api.models.PaymentMethodNonce.parsePaymentMethodNonces;

class TokenizationClient {

    static final String PAYMENT_METHOD_ENDPOINT = "payment_methods";

    /**
     * Create a {@link PaymentMethodNonce} in the Braintree Gateway.
     * <p/>
     * On completion, returns the {@link PaymentMethodNonce} to {@link PaymentMethodNonceCallback}.
     * <p/>
     * If creation fails validation, {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     * will be called with the resulting {@link ErrorWithResponse}.
     * <p/>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)} (Throwable)}
     * will be called with the {@link Exception} that occurred.
     *
     * @param paymentMethodBuilder {@link PaymentMethodBuilder} for the {@link PaymentMethodNonce}
     *        to be created.
     */
    static void tokenize(final BraintreeFragment fragment, final PaymentMethodBuilder paymentMethodBuilder,
            final PaymentMethodNonceCallback callback) {
        paymentMethodBuilder.setSessionId(fragment.getSessionId());

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (paymentMethodBuilder instanceof CardBuilder &&
                        VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP &&
                        configuration.getGraphQL().isFeatureEnabled(Features.TOKENIZE_CREDIT_CARDS)) {
                    tokenizeGraphQL(fragment, (CardBuilder) paymentMethodBuilder, callback);
                } else {
                    tokenizeRest(fragment, paymentMethodBuilder, callback);
                }
            }
        });
    }

    static String versionedPath(String path) {
        return "/v1/" + path;
    }

    private static void tokenizeGraphQL(final BraintreeFragment fragment, final CardBuilder cardBuilder,
            final PaymentMethodNonceCallback callback) {
        fragment.sendAnalyticsEvent("card.graphql.tokenization.started");
        String payload;
        try {
            payload = cardBuilder.buildGraphQL(fragment.getApplicationContext(), fragment.getAuthorization());
        } catch (BraintreeException e) {
            callback.failure(e);
            return;
        }

        fragment.getGraphQLHttpClient().post(payload, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    callback.success(parsePaymentMethodNonces(responseBody, cardBuilder.getResponsePaymentMethodType()));
                    fragment.sendAnalyticsEvent("card.graphql.tokenization.success");
                } catch (JSONException e) {
                    callback.failure(e);
                }
            }

            @Override
            public void failure(Exception exception) {
                fragment.sendAnalyticsEvent("card.graphql.tokenization.failure");
                callback.failure(exception);
            }
        });
    }

    private static void tokenizeRest(final BraintreeFragment fragment, final PaymentMethodBuilder paymentMethodBuilder,
            final PaymentMethodNonceCallback callback) {
        fragment.getHttpClient().post(TokenizationClient.versionedPath(
                TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + paymentMethodBuilder.getApiPath()),
                paymentMethodBuilder.build(), new HttpResponseCallback() {
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
}
