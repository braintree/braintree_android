package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to tokenize credit or debit cards using a {@link Card}. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/credit-cards/overview">documentation</a>
 */
public class CardClient {

    private final BraintreeClient braintreeClient;
    private final DataCollector dataCollector;
    private final TokenizationClient tokenizationClient;

    public CardClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new TokenizationClient(braintreeClient), new DataCollector(braintreeClient));
    }

    @VisibleForTesting
    CardClient(BraintreeClient braintreeClient, TokenizationClient tokenizationClient, DataCollector dataCollector) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
        this.dataCollector = dataCollector;
    }

    /**
     * Create a {@link CardNonce}.
     * <p>
     * The tokenization result is returned via a {@link CardTokenizeCallback} callback.
     *
     * <p>
     * On success, the {@link CardTokenizeCallback#onResult(CardNonce, Exception)} method will
     * be invoked with a nonce.
     *
     * <p>
     * If creation fails validation, the {@link CardTokenizeCallback#onResult(CardNonce, Exception)}
     * method will be invoked with an {@link ErrorWithResponse} exception.
     *
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs, the
     * {@link CardTokenizeCallback#onResult(CardNonce, Exception)} method will be invoked with
     * an {@link Exception} describing the error.
     *
     * @param context Android Context
     * @param card {@link Card}
     * @param callback {@link CardTokenizeCallback}
     */
    public void tokenize(final Context context, final Card card, final CardTokenizeCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (error != null) {
                    callback.onResult(null, error);
                    return;
                }

                boolean shouldTokenizeViaGraphQL =
                    configuration.isGraphQLFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS);

                if (shouldTokenizeViaGraphQL) {
                    card.setSessionId(braintreeClient.getSessionId());
                    try {
                        JSONObject tokenizePayload = card.buildJSONForGraphQL();
                        tokenizationClient.tokenizeGraphQL(tokenizePayload, new TokenizeCallback() {
                            @Override
                            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                                handleTokenizeResponse(context, tokenizationResponse, exception, callback);
                            }
                        });
                    } catch (BraintreeException | JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    tokenizationClient.tokenizeREST(card, new TokenizeCallback() {
                        @Override
                        public void onResult(JSONObject tokenizationResponse, Exception exception) {
                            handleTokenizeResponse(context, tokenizationResponse, exception, callback);
                        }
                    });
                }
            }
        });
    }

    private void handleTokenizeResponse(Context context, JSONObject tokenizationResponse, Exception exception, CardTokenizeCallback callback) {
        if (tokenizationResponse != null) {
            try {
                CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);
                // call to risk can be removed, we don't use this anymore
                dataCollector.collectRiskData(context, cardNonce);

                callback.onResult(cardNonce, null);
                braintreeClient.sendAnalyticsEvent("card.nonce-received");

            } catch (JSONException e) {
                callback.onResult(null, e);
                braintreeClient.sendAnalyticsEvent("card.nonce-failed");
            }
        } else {
            callback.onResult(null, exception);
            braintreeClient.sendAnalyticsEvent("card.nonce-failed");
        }
    }
}
