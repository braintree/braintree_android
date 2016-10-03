package com.braintreepayments.api.models;

import android.support.annotation.Nullable;

import com.braintreepayments.api.Json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

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
    private static final String KOUNT_KEY = "kount";
    private static final String ANDROID_PAY_KEY = "androidPay";
    private static final String THREE_D_SECURE_ENABLED_KEY = "threeDSecureEnabled";
    private static final String PAY_WITH_VENMO_KEY = "payWithVenmo";
    private static final String UNIONPAY_KEY = "unionPay";
    private static final String CARD_KEY = "creditCards";

    private String mConfigurationString;
    private String mClientApiUrl;
    private final Set<String> mChallenges = new HashSet<>();
    private String mEnvironment;
    private String mMerchantId;
    private String mMerchantAccountId;
    private AnalyticsConfiguration mAnalyticsConfiguration;
    private CardConfiguration mCardConfiguration;
    private boolean mPaypalEnabled;
    private PayPalConfiguration mPayPalConfiguration;
    private AndroidPayConfiguration mAndroidPayConfiguration;
    private boolean mThreeDSecureEnabled;
    private VenmoConfiguration mVenmoConfiguration;
    private KountConfiguration mKountConfiguration;
    private UnionPayConfiguration mUnionPayConfiguration;

    /**
     * Creates a new {@link com.braintreepayments.api.models.Configuration} instance from a json string.
     *
     * @param configurationString The json configuration string from Braintree.
     * @return {@link com.braintreepayments.api.models.Configuration} instance.
     */
    public static Configuration fromJson(@Nullable String configurationString) throws JSONException {
        return new Configuration(configurationString);
    }

    protected Configuration(@Nullable String configurationString) throws JSONException {
        if (configurationString == null) {
            throw new JSONException("Configuration cannot be null");
        }

        mConfigurationString = configurationString;
        JSONObject json = new JSONObject(configurationString);

        mClientApiUrl = json.getString(CLIENT_API_URL_KEY);
        parseJsonChallenges(json.optJSONArray(CHALLENGES_KEY));
        mEnvironment = json.getString(ENVIRONMENT_KEY);
        mMerchantId = json.getString(MERCHANT_ID_KEY);
        mMerchantAccountId = Json.optString(json, MERCHANT_ACCOUNT_ID_KEY, null);
        mAnalyticsConfiguration = AnalyticsConfiguration.fromJson(json.optJSONObject(ANALYTICS_KEY));
        mCardConfiguration = CardConfiguration.fromJson(json.optJSONObject(CARD_KEY));
        mPaypalEnabled = json.optBoolean(PAYPAL_ENABLED_KEY, false);
        mPayPalConfiguration = PayPalConfiguration.fromJson(json.optJSONObject(PAYPAL_KEY));
        mAndroidPayConfiguration = AndroidPayConfiguration.fromJson(json.optJSONObject(ANDROID_PAY_KEY));
        mThreeDSecureEnabled = json.optBoolean(THREE_D_SECURE_ENABLED_KEY, false);
        mVenmoConfiguration = VenmoConfiguration.fromJson(json.optJSONObject(PAY_WITH_VENMO_KEY));
        mKountConfiguration = KountConfiguration.fromJson(json.optJSONObject(KOUNT_KEY));
        mUnionPayConfiguration = UnionPayConfiguration.fromJson(json.optJSONObject(UNIONPAY_KEY));
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
        return mChallenges.contains("cvv");
    }

    /**
     * @return {@code true} if postal code is required for card transactions, {@code false} otherwise.
     */
    public boolean isPostalCodeChallengePresent() {
        return mChallenges.contains("postal_code");
    }

    /**
     * @return The current environment.
     */
    public String getEnvironment() {
        return mEnvironment;
    }

    /**
     * @return instance of {@link CardConfiguration}.
     */
    public CardConfiguration getCardConfiguration() {
        return mCardConfiguration;
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

    /**
     * @return instance of {@link VenmoConfiguration}
     */
    public VenmoConfiguration getPayWithVenmo() {
        return mVenmoConfiguration;
    }

    /**
     * @return instance of {@link UnionPayConfiguration}
     */
    public UnionPayConfiguration getUnionPay() {
        return mUnionPayConfiguration;
    }

    /**
     * @return instance of {@link KountConfiguration}.
     */
    public KountConfiguration getKount() {
        return mKountConfiguration;
    }

    private void parseJsonChallenges(JSONArray jsonArray) {
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                mChallenges.add(jsonArray.optString(i, ""));
            }
        }
    }
}
