package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

/**
 * Used to tokenize credit or debit cards using a {@link CardBuilder}. For more information see the
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
     * @param cardBuilder {@link CardBuilder}
     * @param callback {@link CardTokenizeCallback}
     */
    public void tokenize(final Context context, final CardBuilder cardBuilder, final CardTokenizeCallback callback) {
        tokenizationClient.tokenize(cardBuilder, new TokenizeCallback() {
            @Override
            public void onResult(TokenizationResult tokenizationResult, Exception error) {
                if (error == null) {
                    dataCollector.collectRiskData(context, tokenizationResult.getNonce());

                    callback.onResult(CardNonce.from(tokenizationResult), null);
                    braintreeClient.sendAnalyticsEvent("card.nonce-received");
                } else {
                    callback.onResult(null, error);
                    braintreeClient.sendAnalyticsEvent("card.nonce-failed");
                }
            }
        });
    }
}
