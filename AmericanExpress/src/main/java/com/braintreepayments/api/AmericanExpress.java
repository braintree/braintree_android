package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.AmericanExpressRewardsBalance;

import org.json.JSONException;

/**
 * Used to integrate with Braintree's American Express API
 */
// TODO: Rename class when API is finalized
public class AmericanExpress {

    private static final String AMEX_REWARDS_BALANCE_PATH = TokenizationClient.versionedPath(
            "payment_methods/amex_rewards_balance");

    private final BraintreeClient braintreeClient;

    public AmericanExpress(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Gets the rewards balance associated with a Braintree nonce. Only for American Express cards.
     *
     * @param context Android context
     * @param nonce A nonce representing a card that will be used to look up the rewards balance
     * @param currencyIsoCode The currencyIsoCode to use. Example: 'USD'
     * @param callback {@link AmericanExpressGetRewardsBalanceCallback} used to notify result
     */
    public void getRewardsBalance(final Context context, String nonce, String currencyIsoCode, final AmericanExpressGetRewardsBalanceCallback callback) {
        String getRewardsBalanceUrl = Uri.parse(AMEX_REWARDS_BALANCE_PATH)
                .buildUpon()
                .appendQueryParameter("paymentMethodNonce", nonce)
                .appendQueryParameter("currencyIsoCode", currencyIsoCode)
                .build()
                .toString();

        braintreeClient.sendAnalyticsEvent(context, "amex.rewards-balance.start");
        braintreeClient.sendGET(getRewardsBalanceUrl, context, new HttpResponseCallback() {

            @Override
            public void success(String responseBody) {
                braintreeClient.sendAnalyticsEvent(context, "amex.rewards-balance.success");
                try {
                    AmericanExpressRewardsBalance rewardsBalance = AmericanExpressRewardsBalance.fromJson(responseBody);
                    callback.onResult(rewardsBalance, null);
                } catch (JSONException e) {
                    braintreeClient.sendAnalyticsEvent(context, "amex.rewards-balance.parse.failed");
                    callback.onResult(null, e);
                }
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, exception);
                braintreeClient.sendAnalyticsEvent(context, "amex.rewards-balance.error");
            }
        });
    }
}
