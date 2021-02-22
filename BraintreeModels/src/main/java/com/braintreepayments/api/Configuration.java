package com.braintreepayments.api;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final String GOOGLE_PAY_KEY = "androidPay";
    private static final String THREE_D_SECURE_ENABLED_KEY = "threeDSecureEnabled";
    private static final String PAY_WITH_VENMO_KEY = "payWithVenmo";
    private static final String UNIONPAY_KEY = "unionPay";
    private static final String CARD_KEY = "creditCards";
    private static final String VISA_CHECKOUT_KEY = "visaCheckout";
    private static final String GRAPHQL_KEY = "graphQL";
    private static final String SAMSUNG_PAY_KEY = "samsungPay";
    private static final String CARDINAL_AUTHENTICATION_JWT = "cardinalAuthenticationJWT";

    private final String mAssetsUrl;
    private final String mConfigurationString;
    private final String mClientApiUrl;
    private final Set<String> mChallenges = new HashSet<>();
    private final String mEnvironment;
    private final String mMerchantId;
    private final String mMerchantAccountId;
    private final BraintreeApiConfiguration mBraintreeApiConfiguration;
    private final AnalyticsConfiguration mAnalyticsConfiguration;
    private final CardConfiguration mCardConfiguration;
    private final boolean mPaypalEnabled;
    private final PayPalConfiguration mPayPalConfiguration;
    private final GooglePayConfiguration mGooglePayConfiguration;
    private final boolean mThreeDSecureEnabled;
    private final VenmoConfiguration mVenmoConfiguration;
    private final KountConfiguration mKountConfiguration;
    private final UnionPayConfiguration mUnionPayConfiguration;
    private final VisaCheckoutConfiguration mVisaCheckoutConfiguration;
    private final GraphQLConfiguration mGraphQLConfiguration;
    private final SamsungPayConfiguration mSamsungPayConfiguration;
    private final String mCardinalAuthenticationJwt;

    /**
     * Creates a new {@link Configuration} instance from a json string.
     *
     * @param configurationString The json configuration string from Braintree.
     * @return {@link Configuration} instance.
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
        mGooglePayConfiguration = GooglePayConfiguration.fromJson(json.optJSONObject(GOOGLE_PAY_KEY));
        mThreeDSecureEnabled = json.optBoolean(THREE_D_SECURE_ENABLED_KEY, false);
        mVenmoConfiguration = VenmoConfiguration.fromJson(json.optJSONObject(PAY_WITH_VENMO_KEY));
        mKountConfiguration = KountConfiguration.fromJson(json.optJSONObject(KOUNT_KEY));
        mUnionPayConfiguration = UnionPayConfiguration.fromJson(json.optJSONObject(UNIONPAY_KEY));
        mVisaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(json.optJSONObject(VISA_CHECKOUT_KEY));
        mGraphQLConfiguration = GraphQLConfiguration.fromJson(json.optJSONObject(GRAPHQL_KEY));
        mSamsungPayConfiguration = SamsungPayConfiguration.fromJson(json.optJSONObject(SAMSUNG_PAY_KEY));
        mCardinalAuthenticationJwt = Json.optString(json, CARDINAL_AUTHENTICATION_JWT, null);
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
     * @return {@code true} if fraud device data collection should occur; {@code false} otherwise.
     */
    boolean isFraudDataCollectionEnabled() {
        return mCardConfiguration.isFraudDataCollectionEnabled();
    }

    /**
     * @return {@code true} if Venmo is enabled for the merchant account; {@code false} otherwise.
     */
    public boolean isVenmoEnabled() {
        return mVenmoConfiguration.isAccessTokenValid();
    }

    /**
     * @return the Access Token used by the Venmo app to tokenize on behalf of the merchant.
     */
    String getVenmoAccessToken() {
        return mVenmoConfiguration.getAccessToken();
    }

    /**
     * @return the Venmo merchant id used by the Venmo app to authorize payment.
     */
    String getVenmoMerchantId() {
        return mVenmoConfiguration.getMerchantId();
    }

    /**
     * @return the Venmo environment used to handle this payment.
     */
    String getVenmoEnvironment() {
        return mVenmoConfiguration.getEnvironment();
    }

    /**
     * @return {@code true} if GraphQL is enabled for the merchant account; {@code false} otherwise.
     */
    boolean isGraphQLEnabled() {
        return mGraphQLConfiguration.isEnabled();
    }

    /**
     * @return {@code true} if Local Payment is enabled for the merchant account; {@code false} otherwise.
     */
    public boolean isLocalPaymentEnabled() {
        // Local Payments are enabled when PayPal is enabled
        return isPayPalEnabled();
    }

    /**
     * @return {@code true} if Kount is enabled for the merchant account; {@code false} otherwise.
     */
    boolean isKountEnabled() {
        return mKountConfiguration.isEnabled();
    }

    /**
     * @return the Kount merchant id set in the Gateway.
     */
    String getKountMerchantId() {
        return mKountConfiguration.getKountMerchantId();
    }

    /**
     * @return {@code true} if UnionPay is enabled for the merchant account; {@code false} otherwise.
     */
    public boolean isUnionPayEnabled() {
        return mUnionPayConfiguration.isEnabled();
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
        return mPaypalEnabled;
    }

    /**
     * @return the PayPal app display name.
     */
    String getPayPalDisplayName() {
        return mPayPalConfiguration.getDisplayName();
    }

    /**
     * @return the PayPal app client id.
     */
    String getPayPalClientId() {
        return mPayPalConfiguration.getClientId();
    }

    /**
     * @return the PayPal app privacy url.
     */
    public String getPayPalPrivacyUrl() {
        return mPayPalConfiguration.getPrivacyUrl();
    }

    /**
     * @return the PayPal app user agreement url.
     */
    public String getPayPalUserAgreementUrl() {
        return mPayPalConfiguration.getUserAgreementUrl();
    }

    /**
     * @return the url for custom PayPal environments.
     */
    public String getPayPalDirectBaseUrl() {
        return mPayPalConfiguration.getDirectBaseUrl();
    }

    /**
     * @return the current environment for PayPal.
     */
    String getPayPalEnvironment() {
        return mPayPalConfiguration.getEnvironment();
    }

    /**
     * @return {@code true} if PayPal touch is currently disabled, {@code false} otherwise.
     */
    boolean isPayPalTouchDisabled() {
        return mPayPalConfiguration.isTouchDisabled();
    }

    /**
     * @return the PayPal currency code.
     */
    String getPayPalCurrencyIsoCode() {
        return mPayPalConfiguration.getCurrencyIsoCode();
    }

    /**
     * @return {@code true} if Google Payment is enabled and supported in the current environment; {@code false} otherwise.
     */
    public boolean isGooglePayEnabled() {
        return mGooglePayConfiguration.isEnabled();
    }

    /**
     * @return the authorization fingerprint to use for Google Payment, only allows tokenizing Google Payment cards.
     */
    String getGooglePayAuthorizationFingerprint() {
        return mGooglePayConfiguration.getGoogleAuthorizationFingerprint();
    }

    /**
     * @return the current Google Pay environment.
     */
    String getGooglePayEnvironment() {
        return mGooglePayConfiguration.getEnvironment();
    }

    /**
     * @return the Google Pay display name to show to the user.
     */
    String getGooglePayDisplayName() {
        return mGooglePayConfiguration.getDisplayName();
    }

    /**
     * @return a list of supported card networks for Google Pay.
     */
    List<String> getGooglePaySupportedNetworks() {
        return mGooglePayConfiguration.getSupportedNetworks();
    }

    /**
     * @return the PayPal Client ID used by Google Pay.
     */
    String getGooglePayPayPalClientId() {
        return mGooglePayConfiguration.getPaypalClientId();
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
     * @return {@link String} url of the Braintree analytics service.
     */
    String getAnalyticsUrl() {
        return mAnalyticsConfiguration.getUrl();
    }

    /**
     * @return {@code true} if analytics are enabled, {@code false} otherwise.
     */
    boolean isAnalyticsEnabled() {
        return mAnalyticsConfiguration.isEnabled();
    }

    /**
     * @return {@code true} if Visa Checkout is enabled for the merchant account; {@code false} otherwise.
     */
    public boolean isVisaCheckoutEnabled() {
        return mVisaCheckoutConfiguration.isEnabled();
    }

    /**
     * @return the Visa Checkout supported networks enabled for the merchant account.
     */
    List<String> getVisaCheckoutSupportedNetworks() {
        return mVisaCheckoutConfiguration.getAcceptedCardBrands();
    }

    /**
     * @return the Visa Checkout API key configured in the Braintree Control Panel.
     */
    String getVisaCheckoutApiKey() {
        return mVisaCheckoutConfiguration.getApiKey();
    }

    /**
     * @return the Visa Checkout External Client ID configured in the Braintree Control Panel.
     */
    String getVisaCheckoutExternalClientId() {
        return mVisaCheckoutConfiguration.getExternalClientId();
    }

    /**
     * Check if a specific feature is enabled in the GraphQL API.
     *
     * @param feature The feature to check.
     * @return {@code true} if GraphQL is enabled and the feature is enabled, {@code false} otherwise.
     */
    boolean isGraphQLFeatureEnabled(String feature) {
        return mGraphQLConfiguration.isFeatureEnabled(feature);
    }

    /**
     * @return the GraphQL url.
     */
    String getGraphQLUrl() {
        return mGraphQLConfiguration.getUrl();
    }

    /**
     * @return {@code true} if Samsung Pay is enabled; {@code false} otherwise.
     */
    public boolean isSamsungPayEnabled() {
        return mSamsungPayConfiguration.isEnabled();
    }

    /**
     * @return the merchant display name for Samsung Pay.
     */
    String getSamsungPayMerchantDisplayName() {
        return mSamsungPayConfiguration.getMerchantDisplayName();
    }

    /**
     * @return the Samsung Pay service id associated with the merchant.
     */
    String getSamsungPayServiceId() {
        return mSamsungPayConfiguration.getServiceId();
    }

    /**
     * @return a list of card brands supported by Samsung Pay.
     */
    List<String> getSamsungPaySupportedCardBrands() {
        return new ArrayList<>(mSamsungPayConfiguration.getSupportedCardBrands());
    }

    /**
     * @return the authorization to use with Samsung Pay.
     */
    String getSamsungPayAuthorization() {
        return mSamsungPayConfiguration.getSamsungAuthorization();
    }

    /**
     * @return the Braintree environment Samsung Pay should interact with.
     */
    String getSamsungPayEnvironment() {
        return mSamsungPayConfiguration.getEnvironment();
    }

    private void parseJsonChallenges(JSONArray jsonArray) {
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                mChallenges.add(jsonArray.optString(i, ""));
            }
        }
    }

    /**
     * @return the JWT for Cardinal
     */
    public String getCardinalAuthenticationJwt() {
        return mCardinalAuthenticationJwt;
    }

    /**
     * @return The Access Token for Braintree API.
     */
    String getBraintreeApiAccessToken() {
        return mBraintreeApiConfiguration.getAccessToken();
    }

    /**
     * @return the base url for accessing Braintree API.
     */
    String getBraintreeApiUrl() {
        return mBraintreeApiConfiguration.getUrl();
    }

    /**
     * @return a boolean indicating whether Braintree API is enabled for this merchant.
     */
    boolean isBraintreeApiEnabled() {
        return mBraintreeApiConfiguration.isEnabled();
    }

    /**
     * @return a {@link List<String>} of card types supported by the merchant.
     */
    List<String> getSupportedCardTypes() {
        return mCardConfiguration.getSupportedCardTypes();
    }
}
