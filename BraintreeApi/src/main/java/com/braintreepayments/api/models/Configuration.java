package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.Gson;

/**
 * Contains the remote configuration for the Braintree Android SDK.
 */
public class Configuration {

    private String[] challenges;
    private boolean paypalEnabled;
    private PayPalConfiguration paypal;
    private String venmo;
    private boolean coinbaseEnabled;
    private CoinbaseConfiguration coinbase;
    private boolean threeDSecureEnabled;
    private String merchantId;
    private String merchantAccountId;
    private AnalyticsConfiguration analytics;

    /**
     * Creates a new {@link com.braintreepayments.api.models.Configuration} instance from a json string.
     *
     * @param configuration The json configuration string from Braintree.
     * @return {@link com.braintreepayments.api.models.Configuration} instance.
     */
    public static Configuration fromJson(String configuration) {
        return new Gson().fromJson(configuration, Configuration.class);
    }

    /**
     * @return {@code true} if cvv is required for card transactions, {@code false} otherwise.
     */
    public boolean isCvvChallengePresent() {
        return isChallengePresent("cvv");
    }

    /**
     * @return {@code true} if postal code is required for card transactions, {@code false} otherwise.
     */
    public boolean isPostalCodeChallengePresent() {
        return isChallengePresent("postal_code");
    }

    /**
     * @return {@code true} if PayPal is enabled, {@code false} otherwise.
     */
    public boolean isPayPalEnabled() {
        return (paypalEnabled && paypal != null);
    }

    /**
     * @return instance of {@link com.braintreepayments.api.models.PayPalConfiguration}.
     */
    public PayPalConfiguration getPayPal() {
        return paypal;
    }

    /**
     * @return a {@link java.lang.String} of "off" is Venmo is disabled, a {@link String} of
     * "offline" when the Venmo environment is offline or a {@link String} of "live" when the
     * Venmo environment is live.
     */
    public String getVenmoState() {
        if (venmo == null) {
            return "off";
        } else {
            return venmo;
        }
    }

    /**
     * @return {@code true} if Coinbase is enabled, {@code false} otherwise.
     */
    public boolean isCoinbaseEnabled() {
        return (coinbaseEnabled && coinbase != null);
    }

    /**
     * @return instance of {@link com.braintreepayments.api.models.CoinbaseConfiguration}.
     */
    public CoinbaseConfiguration getCoinbase() {
        return coinbase;
    }

    /**
     * @return {@code true} if 3D Secure is enabled, {@code false} otherwise.
     */
    @Beta
    public boolean isThreeDSecureEnabled() {
        return threeDSecureEnabled;
    }

    /**
     * @return the current Braintree merchant id.
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * @return the current Braintree merchant account id.
     */
    public String getMerchantAccountId() {
        return merchantAccountId;
    }

    /**
     * @return {@code true} if analytics are enabled, {@code false} otherwise.
     */
    public boolean isAnalyticsEnabled() {
        return (analytics != null && !TextUtils.isEmpty(analytics.getUrl()));
    }

    /**
     * @return instance of {@link com.braintreepayments.api.models.AnalyticsConfiguration}.
     */
    public AnalyticsConfiguration getAnalytics() {
        return analytics;
    }

    private boolean isChallengePresent(String requestedChallenge) {
        if (challenges != null && challenges.length > 0) {
            for (String challenge : challenges) {
                if (challenge.equals(requestedChallenge)) {
                    return true;
                }
            }
        }

        return false;
    }
}
