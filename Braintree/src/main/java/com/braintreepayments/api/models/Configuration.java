package com.braintreepayments.api.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private static final String ASSETS_URL_KEY = "assetsUrl";
    private static final String CLIENT_API_URL_KEY = "clientApiUrl";
    private static final String CHALLENGES_KEY = "challenges";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String MERCHANT_ID_KEY = "merchantId";
    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId";
    private static final String ANALYTICS_KEY = "analytics";
    private static final String BRAINTREE_API_KEY = "braintreeApi";
    private static final String PAYPAL_ENABLED_KEY = "paypalEnabled";
    private static final String PAYPAL_KEY = "paypal";
    private static final String KOUNT_KEY = "kount";
    private static final String GOOGLE_PAYMENT_KEY = "androidPay";
    private static final String THREE_D_SECURE_ENABLED_KEY = "threeDSecureEnabled";
    private static final String PAY_WITH_VENMO_KEY = "payWithVenmo";
    private static final String UNIONPAY_KEY = "unionPay";
    private static final String CARD_KEY = "creditCards";
    private static final String VISA_CHECKOUT_KEY = "visaCheckout";
    private static final String IDEAL_KEY = "ideal";
    private static final String GRAPHQL_KEY = "graphQL";
    private static final String SAMSUNG_PAY_KEY = "samsungPay";

    private String mAssetsUrl;
    private String mConfigurationString;
    private String mClientApiUrl;
    private final Set<String> mChallenges = new HashSet<>();
    private String mEnvironment;
    private String mMerchantId;
    private String mMerchantAccountId;
    private BraintreeApiConfiguration mBraintreeApiConfiguration;
    private IdealConfiguration mIdealConfiguration;
    private AnalyticsConfiguration mAnalyticsConfiguration;
    private CardConfiguration mCardConfiguration;
    private boolean mPaypalEnabled;
    private PayPalConfiguration mPayPalConfiguration;
    private GooglePaymentConfiguration mGooglePaymentConfiguration;
    private boolean mThreeDSecureEnabled;
    private VenmoConfiguration mVenmoConfiguration;
    private KountConfiguration mKountConfiguration;
    private UnionPayConfiguration mUnionPayConfiguration;
    private VisaCheckoutConfiguration mVisaCheckoutConfiguration;
    private GraphQLConfiguration mGraphQLConfiguration;
    private SamsungPayConfiguration mSamsungPayConfiguration;

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

        mAssetsUrl = Json.optString(json, ASSETS_URL_KEY, "");
        mClientApiUrl = json.getString(CLIENT_API_URL_KEY);
        parseJsonChallenges(json.optJSONArray(CHALLENGES_KEY));
        mEnvironment = json.getString(ENVIRONMENT_KEY);
        mMerchantId = json.getString(MERCHANT_ID_KEY);
        mMerchantAccountId = Json.optString(json, MERCHANT_ACCOUNT_ID_KEY, null);
        mAnalyticsConfiguration = AnalyticsConfiguration.fromJson(json.optJSONObject(ANALYTICS_KEY));
        mBraintreeApiConfiguration = BraintreeApiConfiguration.fromJson(json.optJSONObject(BRAINTREE_API_KEY));
        mCardConfiguration = CardConfiguration.fromJson(json.optJSONObject(CARD_KEY));
        mPaypalEnabled = json.optBoolean(PAYPAL_ENABLED_KEY, false);
        mPayPalConfiguration = PayPalConfiguration.fromJson(json.optJSONObject(PAYPAL_KEY));
        mGooglePaymentConfiguration = GooglePaymentConfiguration.fromJson(json.optJSONObject(GOOGLE_PAYMENT_KEY));
        mThreeDSecureEnabled = json.optBoolean(THREE_D_SECURE_ENABLED_KEY, false);
        mVenmoConfiguration = VenmoConfiguration.fromJson(json.optJSONObject(PAY_WITH_VENMO_KEY));
        mKountConfiguration = KountConfiguration.fromJson(json.optJSONObject(KOUNT_KEY));
        mUnionPayConfiguration = UnionPayConfiguration.fromJson(json.optJSONObject(UNIONPAY_KEY));
        mVisaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(json.optJSONObject(VISA_CHECKOUT_KEY));
        mIdealConfiguration = IdealConfiguration.fromJson(json.optJSONObject(IDEAL_KEY));
        mGraphQLConfiguration = GraphQLConfiguration.fromJson(json.optJSONObject(GRAPHQL_KEY));
        mSamsungPayConfiguration = SamsungPayConfiguration.fromJson(json.optJSONObject(SAMSUNG_PAY_KEY));
    }

    public String toJson() {
        return mConfigurationString;
    }

    /**
     * @return The assets URL of the current environment.
     */
    public String getAssetsUrl() {
        return mAssetsUrl;
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
     * @return instance of {@link BraintreeApiConfiguration}.
     */
    public BraintreeApiConfiguration getBraintreeApiConfiguration() {
        return mBraintreeApiConfiguration;
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
     * @return instance of {@link GooglePaymentConfiguration}.
     */
    public GooglePaymentConfiguration getGooglePayment() {
        return mGooglePaymentConfiguration;
    }

    /**
     * @deprecated Use {@link #getGooglePayment()}.
     */
    @Deprecated
    public AndroidPayConfiguration getAndroidPay() {
        return getGooglePayment();
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
     * @return instance of {@link VisaCheckoutConfiguration}
     */
    public VisaCheckoutConfiguration getVisaCheckout() {
        return mVisaCheckoutConfiguration;
    }

    /**
     * @return instance of {@link KountConfiguration}.
     */
    public KountConfiguration getKount() {
        return mKountConfiguration;
    }

    /**
     * @return instance of {@link IdealConfiguration}.
     */
    public IdealConfiguration getIdealConfiguration() {
        return mIdealConfiguration;
    }

    /**
     * @return instance of {@link GraphQLConfiguration}.
     */
    public GraphQLConfiguration getGraphQL() {
        return mGraphQLConfiguration;
    }

    /**
     * @return instance of {@link SamsungPayConfiguration}.
     */
    @NonNull
    public SamsungPayConfiguration getSamsungPay() {
        return mSamsungPayConfiguration;
    }

    private void parseJsonChallenges(JSONArray jsonArray) {
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                mChallenges.add(jsonArray.optString(i, ""));
            }
        }
    }
}
