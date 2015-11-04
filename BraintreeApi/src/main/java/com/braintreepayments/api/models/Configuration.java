package com.braintreepayments.api.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains the remote configuration for the Braintree Android SDK.
 */
public class Configuration {

    private static final String CLIENT_API_URL_KEY = "clientApiUrl";
    private static final String CHALLENGES_KEY = "challenges";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String MERCHANT_ID_KEY = "merchantId";
    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId";
    private static final String ANALYTICS_KEY = "analytics";
    private static final String PAYPAL_ENABLED_KEY = "paypalEnabled";
    private static final String PAYPAL_KEY = "paypal";
    private static final String ANDROID_PAY_KEY = "androidPay";
    private static final String THREE_D_SECURE_ENABLED_KEY = "threeDSecureEnabled";

    private String mConfigurationString;
    private String mClientApiUrl;
    private String[] mChallenges;
    private String mEnvironment;
    private String mMerchantId;
    private String mMerchantAccountId;
    private AnalyticsConfiguration mAnalyticsConfiguration;
    private boolean mPaypalEnabled;
    private PayPalConfiguration mPayPalConfiguration;
    private AndroidPayConfiguration mAndroidPayConfiguration;
    private boolean mThreeDSecureEnabled;

    /**
     * Creates a new {@link com.braintreepayments.api.models.Configuration} instance from a json string.
     *
     * @param configurationString The json configuration string from Braintree.
     * @return {@link com.braintreepayments.api.models.Configuration} instance.
     */
    public static Configuration fromJson(String configurationString) throws JSONException {
        Configuration configuration = new Configuration();
        configuration.mConfigurationString = configurationString;
        JSONObject json = new JSONObject(configurationString);

        configuration.mClientApiUrl = json.getString(CLIENT_API_URL_KEY);
        configuration.mChallenges = parseJsonChallenges(json.optJSONArray(CHALLENGES_KEY));
        configuration.mEnvironment = json.getString(ENVIRONMENT_KEY);
        configuration.mPaypalEnabled = json.optBoolean(PAYPAL_ENABLED_KEY, false);
        configuration.mPayPalConfiguration = PayPalConfiguration.fromJson(json.optJSONObject(PAYPAL_KEY));
        configuration.mAndroidPayConfiguration = AndroidPayConfiguration.fromJson(json.optJSONObject(ANDROID_PAY_KEY));
        configuration.mThreeDSecureEnabled = json.optBoolean(THREE_D_SECURE_ENABLED_KEY, false);
        configuration.mMerchantId = json.getString(MERCHANT_ID_KEY);
        configuration.mMerchantAccountId = json.optString(MERCHANT_ACCOUNT_ID_KEY, null);
        configuration.mAnalyticsConfiguration = AnalyticsConfiguration.fromJson(json.optJSONObject(ANALYTICS_KEY));

        return configuration;
    }

    public String toJson() {
        return mConfigurationString;
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
     * @return The current environment.
     */
    public String getEnvironment() {
        return mEnvironment;
    }

    /**
     * @return {@code true} if PayPal is enabled and supported in the current environment,
     *         {@code false} otherwise.
     */
    public boolean isPayPalEnabled() {
        return (mPaypalEnabled && mPayPalConfiguration.isEnabled());
    }

    /**
     * @return instance of {@link com.braintreepayments.api.models.PayPalConfiguration}.
     */
    public PayPalConfiguration getPayPal() {
        return mPayPalConfiguration;
    }

    /**
     * @return instance of {@link AndroidPayConfiguration}.
     */
    public AndroidPayConfiguration getAndroidPay() {
        return mAndroidPayConfiguration;
    }

    /**
     * @return {@code true} if 3D Secure is enabled and supported for the current merchant account,
     *         {@code false} otherwise.
     */
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
     * @return instance of {@link com.braintreepayments.api.models.AnalyticsConfiguration}.
     */
    public AnalyticsConfiguration getAnalytics() {
        return mAnalyticsConfiguration;
    }

    private boolean isChallengePresent(String requestedChallenge) {
        for (String challenge : mChallenges) {
            if (challenge.equals(requestedChallenge)) {
                return true;
            }
        }

        return false;
    }

    private static String[] parseJsonChallenges(JSONArray jsonArray) {
        if (jsonArray == null) {
            return new String[0];
        }

        String[] challenges = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            challenges[i] = jsonArray.optString(i, "");
        }

        return challenges;
    }
}
