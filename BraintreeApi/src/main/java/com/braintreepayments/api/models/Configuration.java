package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Contains the remote configuration for the Braintree Android SDK.
 */
public class Configuration {

    @SerializedName("clientApiUrl") private String mClientApiUrl;
    @SerializedName("challenges") private String[] mChallenges;
    @SerializedName("paypalEnabled") private boolean mPaypalEnabled;
    @SerializedName("paypal") private PayPalConfiguration mPayPalConfiguration;
    @SerializedName("venmo") private String mVenmo;
    @SerializedName("threeDSecureEnabled") private boolean mThreeDSecureEnabled;
    @SerializedName("merchantId") private String mMerchantId;
    @SerializedName("merchantAccountId") private String mMerchantAccountId;
    @SerializedName("analytics") private AnalyticsConfiguration mAnalyticsConfiguration;

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
     * @return The url of the Braintree client API for the current environment.
     */
    public String getClientApiUrl() {
        return mClientApiUrl;
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
        return (mPaypalEnabled && mPayPalConfiguration != null);
    }

    /**
     * @return instance of {@link com.braintreepayments.api.models.PayPalConfiguration}.
     */
    public PayPalConfiguration getPayPal() {
        return mPayPalConfiguration;
    }

    /**
     * @return a {@link java.lang.String} of "off" is Venmo is disabled, a {@link String} of
     * "offline" when the Venmo environment is offline or a {@link String} of "live" when the
     * Venmo environment is live.
     */
    public String getVenmoState() {
        if (mVenmo == null) {
            return "off";
        } else {
            return mVenmo;
        }
    }

    /**
     * @return {@code true} if 3D Secure is enabled, {@code false} otherwise.
     */
    @Beta
    public boolean isThreeDSecureEnabled() {
        return mThreeDSecureEnabled;
    }

    /**
     * @return the current Braintree merchant id.
     */
    public String getMerchantId() {
        return mMerchantId;
    }

    /**
     * @return the current Braintree merchant account id.
     */
    public String getMerchantAccountId() {
        return mMerchantAccountId;
    }

    /**
     * @return {@code true} if analytics are enabled, {@code false} otherwise.
     */
    public boolean isAnalyticsEnabled() {
        return (mAnalyticsConfiguration != null && !TextUtils.isEmpty(mAnalyticsConfiguration.getUrl()));
    }

    /**
     * @return instance of {@link com.braintreepayments.api.models.AnalyticsConfiguration}.
     */
    public AnalyticsConfiguration getAnalytics() {
        return mAnalyticsConfiguration;
    }

    private boolean isChallengePresent(String requestedChallenge) {
        if (mChallenges != null && mChallenges.length > 0) {
            for (String challenge : mChallenges) {
                if (challenge.equals(requestedChallenge)) {
                    return true;
                }
            }
        }

        return false;
    }
}
