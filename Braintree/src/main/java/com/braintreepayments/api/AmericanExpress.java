package com.braintreepayments.api;

import android.net.Uri;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.AmericanExpressRewardsBalance;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;

/**
 * Used to integrate with Braintree's American Express API
 */
public class AmericanExpress {

    private static final String AMEX_REWARDS_BALANCE_PATH = TokenizationClient.versionedPath(
            "payment_methods/amex_rewards_balance");

    /**
     * Gets the rewards balance associated with a Braintree nonce. Only for American Express cards.
     *
     * @param fragment the {@link BraintreeFragment} This fragment will also be responsible
     * for handling callbacks to it's listeners
     * @param nonce A nonce representing a card that will be used to look up the rewards balance
     * @param currencyIsoCode The currencyIsoCode to use. Example: 'USD'
     */
    public static void getRewardsBalance(final BraintreeFragment fragment, final String nonce,
            final String currencyIsoCode) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                String getRewardsBalanceUrl = Uri.parse(AMEX_REWARDS_BALANCE_PATH)
                        .buildUpon()
                        .appendQueryParameter("paymentMethodNonce", nonce)
                        .appendQueryParameter("currencyIsoCode", currencyIsoCode)
                        .build()
                        .toString();

                fragment.sendAnalyticsEvent("amex.rewards-balance.start");
                fragment.getHttpClient().get(getRewardsBalanceUrl, new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        fragment.sendAnalyticsEvent("amex.rewards-balance.success");
                        try {
                            fragment.postAmericanExpressCallback(AmericanExpressRewardsBalance.fromJson(responseBody));
                        } catch (JSONException e) {
                            fragment.sendAnalyticsEvent("amex.rewards-balance.parse.failed");
                            fragment.postCallback(e);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.postCallback(exception);
                        fragment.sendAnalyticsEvent("amex.rewards-balance.error");
                    }
                });
            }
        });
    }
}
