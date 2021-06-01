package com.braintreepayments.api;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.json.JSONException;

/**
 * Used to integrate with Braintree's American Express API
 */
public class AmericanExpressClient {

    private static final String AMEX_REWARDS_BALANCE_PATH =
        TokenizationClient.versionedPath("payment_methods/amex_rewards_balance");

    private final BraintreeClient braintreeClient;

    public AmericanExpressClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Gets the rewards balance associated with a Braintree nonce. Only for American Express cards.
     * @param nonce A nonce representing a card that will be used to look up the rewards balance
     * @param currencyIsoCode The currencyIsoCode to use. Example: 'USD'
     * @param callback {@link AmericanExpressGetRewardsBalanceCallback}
     */
    public void getRewardsBalance(@NonNull String nonce, @NonNull String currencyIsoCode, @NonNull final AmericanExpressGetRewardsBalanceCallback callback) {
        String getRewardsBalanceUrl = Uri.parse(AMEX_REWARDS_BALANCE_PATH)
                .buildUpon()
                .appendQueryParameter("paymentMethodNonce", nonce)
                .appendQueryParameter("currencyIsoCode", currencyIsoCode)
                .build()
                .toString();

        braintreeClient.sendAnalyticsEvent("amex.rewards-balance.start");
        braintreeClient.sendGET(getRewardsBalanceUrl, new HttpResponseCallback() {

            @Override
            public void success(String responseBody) {
                braintreeClient.sendAnalyticsEvent("amex.rewards-balance.success");
                try {
                    AmericanExpressRewardsBalance rewardsBalance = AmericanExpressRewardsBalance.fromJson(responseBody);
                    callback.onResult(rewardsBalance, null);
                } catch (JSONException e) {
                    braintreeClient.sendAnalyticsEvent("amex.rewards-balance.parse.failed");
                    callback.onResult(null, e);
                }
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, exception);
                braintreeClient.sendAnalyticsEvent("amex.rewards-balance.error");
            }
        });
    }
}
