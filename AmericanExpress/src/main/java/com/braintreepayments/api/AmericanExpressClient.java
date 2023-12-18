package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;

/**
 * Used to integrate with Braintree's American Express API
 */
public class AmericanExpressClient {

    private static final String AMEX_REWARDS_BALANCE_PATH =
            ApiClient.versionedPath("payment_methods/amex_rewards_balance");

    private final BraintreeClient braintreeClient;

    /**
     * Initializes a new {@link AmericanExpressClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public AmericanExpressClient(@NonNull Context context, @NonNull String authorization) {
        this.braintreeClient = new BraintreeClient(context, authorization);
    }

    @VisibleForTesting AmericanExpressClient(@NonNull BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Gets the rewards balance associated with a Braintree nonce. Only for American Express cards.
     *
     * @param nonce           A nonce representing a card that will be used to look up the rewards
     *                        balance
     * @param currencyIsoCode The currencyIsoCode to use. Example: 'USD'
     * @param callback        {@link AmericanExpressGetRewardsBalanceCallback}
     */
    public void getRewardsBalance(@NonNull String nonce, @NonNull String currencyIsoCode, @NonNull
    final AmericanExpressGetRewardsBalanceCallback callback) {
        String getRewardsBalanceUrl = Uri.parse(AMEX_REWARDS_BALANCE_PATH)
                .buildUpon()
                .appendQueryParameter("paymentMethodNonce", nonce)
                .appendQueryParameter("currencyIsoCode", currencyIsoCode)
                .build()
                .toString();

        braintreeClient.sendAnalyticsEvent("amex.rewards-balance.start");
        braintreeClient.sendGET(getRewardsBalanceUrl, (responseBody, httpError) -> {
            if (responseBody != null) {
                braintreeClient.sendAnalyticsEvent("amex.rewards-balance.success");
                try {
                    AmericanExpressRewardsBalance rewardsBalance =
                            AmericanExpressRewardsBalance.fromJson(responseBody);
                    callback.onAmericanExpressResult(new AmericanExpressResult.Success(rewardsBalance));
                } catch (JSONException e) {
                    braintreeClient.sendAnalyticsEvent("amex.rewards-balance.parse.failed");
                    callback.onAmericanExpressResult(new AmericanExpressResult.Failure(e));
                }
            } else if (httpError != null) {
                callback.onAmericanExpressResult(new AmericanExpressResult.Failure(httpError));
                braintreeClient.sendAnalyticsEvent("amex.rewards-balance.error");
            }
        });
    }
}
