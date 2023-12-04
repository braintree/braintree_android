package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to tokenize credit or debit cards using a {@link Card}. For more information see the
 * <a
 * href="https://developer.paypal.com/braintree/docs/guides/credit-cards/overview">documentation</a>
 */
public class CardClient {

    private final BraintreeClient braintreeClient;
    private final ApiClient apiClient;

    /**
     * Initializes a new {@link CardClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public CardClient(@NonNull Context context, @NonNull String authorization) {
        this(new BraintreeClient(context, authorization));
    }

    @VisibleForTesting
    CardClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    CardClient(BraintreeClient braintreeClient, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
    }

    /**
     * Create a {@link CardNonce}.
     * <p>
     * The tokenization result is returned via a {@link CardTokenizeCallback} callback.
     *
     * <p>
     * On success, the {@link CardTokenizeCallback#onResult(CardNonce, Exception)} method will be
     * invoked with a nonce.
     *
     * <p>
     * If creation fails validation, the {@link CardTokenizeCallback#onResult(CardNonce, Exception)}
     * method will be invoked with an {@link ErrorWithResponse} exception.
     *
     * <p>
     * If an error not due to validation (server error, network issue, etc.) occurs, the
     * {@link CardTokenizeCallback#onResult(CardNonce, Exception)} method will be invoked with an
     * {@link Exception} describing the error.
     *
     * @param card     {@link Card}
     * @param callback {@link CardTokenizeCallback}
     */
    public void tokenize(@NonNull final Card card, @NonNull final CardTokenizeCallback callback) {
        braintreeClient.getConfiguration((configuration, error) -> {
            if (error != null) {
                callback.onResult(null, error);
                return;
            }

            boolean shouldTokenizeViaGraphQL =
                    configuration.isGraphQLFeatureEnabled(
                            GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS);

            if (shouldTokenizeViaGraphQL) {
                card.setSessionId(braintreeClient.getSessionId());
                try {
                    JSONObject tokenizePayload = card.buildJSONForGraphQL();
                    apiClient.tokenizeGraphQL(tokenizePayload,
                            (tokenizationResponse, exception) -> handleTokenizeResponse(
                                    tokenizationResponse, exception, callback));
                } catch (BraintreeException | JSONException e) {
                    callback.onResult(null, e);
                }
            } else {
                apiClient.tokenizeREST(card,
                        (tokenizationResponse, exception) -> handleTokenizeResponse(
                                tokenizationResponse, exception, callback));
            }
        });
    }

    private void handleTokenizeResponse(JSONObject tokenizationResponse, Exception exception,
                                        CardTokenizeCallback callback) {
        if (tokenizationResponse != null) {
            try {
                CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);

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
