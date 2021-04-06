package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;

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
        tokenizationClient.tokenize(card, new PaymentMethodNonceCallback() {
            @Override
            public void onResult(BraintreeNonce braintreeNonce, Exception exception) {
                if (braintreeNonce != null) {
                    try {
                        CardNonce cardNonce = CardNonce.from(braintreeNonce);
                        dataCollector.collectRiskData(context, cardNonce);

                        callback.onResult(cardNonce, null);
                        braintreeClient.sendAnalyticsEvent("card.nonce-received");
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, exception);
                    braintreeClient.sendAnalyticsEvent("card.nonce-failed");
                }
            }
        });
    }
}
