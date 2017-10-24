package com.braintreepayments.api;

import android.net.Uri;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.AmericanExpressRewardsBalance;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;

public class AmericanExpress {

    private static final String AMEX_REWARDS_BALANCE_PATH = TokenizationClient.versionedPath(
            "payment_methods/amex_rewards_balance");

    /**
     * Gets the rewards balance associated with a Braintree nonce.
     *
     * @param fragment the {@link BraintreeFragment} backing the http request. This fragment will also be responsible
     * for handling callbacks to it's listeners
     * @param nonce The nonce that represents a card that will be used to get the rewards balance
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

                fragment.getHttpClient().get(getRewardsBalanceUrl, new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        fragment.sendAnalyticsEvent("amex.rewards-balance.success");
                        try {
                            AmericanExpressRewardsBalance rewardsBalance =
                                    AmericanExpressRewardsBalance.fromJson(responseBody);
                            fragment.postAmericanExpressCallback(rewardsBalance);
                        } catch (JSONException e) {
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
