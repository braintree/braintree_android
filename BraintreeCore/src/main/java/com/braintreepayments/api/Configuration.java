package com.braintreepayments.api;

import androidx.annotation.NonNull;
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

    private final String assetsUrl;
    private final String configurationString;
    private final String clientApiUrl;
    private final Set<String> challenges = new HashSet<>();
    private final String environment;
    private final String merchantId;
    private final String merchantAccountId;
    private final BraintreeApiConfiguration braintreeApiConfiguration;
    private final AnalyticsConfiguration analyticsConfiguration;
    private final CardConfiguration cardConfiguration;
    private final boolean paypalEnabled;
    private final PayPalConfiguration payPalConfiguration;
    private final GooglePayConfiguration googlePayConfiguration;
    private final boolean threeDSecureEnabled;
    private final VenmoConfiguration venmoConfiguration;
    private final KountConfiguration kountConfiguration;
    private final UnionPayConfiguration unionPayConfiguration;
    private final VisaCheckoutConfiguration visaCheckoutConfiguration;
    private final GraphQLConfiguration graphQLConfiguration;
    private final SamsungPayConfiguration samsungPayConfiguration;
    private final String cardinalAuthenticationJwt;

    /**
     * Creates a new {@link Configuration} instance from a json string.
     *
     * @param configurationString The json configuration string from Braintree.
     * @return {@link Configuration} instance.
     */
    public static Configuration fromJson(@NonNull String configurationString) throws JSONException {
        return new Configuration(configurationString);
    }

    Configuration(String configurationString) throws JSONException {
        if (configurationString == null) {
            throw new JSONException("Configuration cannot be null");
        }

        this.configurationString = configurationString;
        JSONObject json = new JSONObject(configurationString);

        assetsUrl = Json.optString(json, ASSETS_URL_KEY, "");
        clientApiUrl = json.getString(CLIENT_API_URL_KEY);
        parseJsonChallenges(json.optJSONArray(CHALLENGES_KEY));
        environment = json.getString(ENVIRONMENT_KEY);
        merchantId = json.getString(MERCHANT_ID_KEY);
        merchantAccountId = Json.optString(json, MERCHANT_ACCOUNT_ID_KEY, null);
        analyticsConfiguration = AnalyticsConfiguration.fromJson(json.optJSONObject(ANALYTICS_KEY));
        braintreeApiConfiguration = BraintreeApiConfiguration.fromJson(json.optJSONObject(BRAINTREE_API_KEY));
        cardConfiguration = CardConfiguration.fromJson(json.optJSONObject(CARD_KEY));
        paypalEnabled = json.optBoolean(PAYPAL_ENABLED_KEY, false);
        payPalConfiguration = PayPalConfiguration.fromJson(json.optJSONObject(PAYPAL_KEY));
        googlePayConfiguration = GooglePayConfiguration.fromJson(json.optJSONObject(GOOGLE_PAY_KEY));
        threeDSecureEnabled = json.optBoolean(THREE_D_SECURE_ENABLED_KEY, false);
        venmoConfiguration = VenmoConfiguration.fromJson(json.optJSONObject(PAY_WITH_VENMO_KEY));
        kountConfiguration = KountConfiguration.fromJson(json.optJSONObject(KOUNT_KEY));
        unionPayConfiguration = UnionPayConfiguration.fromJson(json.optJSONObject(UNIONPAY_KEY));
        visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(json.optJSONObject(VISA_CHECKOUT_KEY));
        graphQLConfiguration = GraphQLConfiguration.fromJson(json.optJSONObject(GRAPHQL_KEY));
        samsungPayConfiguration = SamsungPayConfiguration.fromJson(json.optJSONObject(SAMSUNG_PAY_KEY));
        cardinalAuthenticationJwt = Json.optString(json, CARDINAL_AUTHENTICATION_JWT, null);
    }

    @NonNull
    public String toJson() {
        return configurationString;
    }

    /**
     * @return The assets URL of the current environment.
     */
    @NonNull
    public String getAssetsUrl() {
        return assetsUrl;
    }

    /**
     * @return The url of the Braintree client API for the current environment.
     */
    @NonNull
    public String getClientApiUrl() {
        return clientApiUrl;
    }

    /**
     * @return {@code true} if cvv is required for card transactions, {@code false} otherwise.
     */
    public boolean isCvvChallengePresent() {
        return challenges.contains("cvv");
    }

    /**
     * @return {@code true} if postal code is required for card transactions, {@code false} otherwise.
     */
    public boolean isPostalCodeChallengePresent() {
        return challenges.contains("postal_code");
    }

    /**
     * @return {@code true} if fraud device data collection should occur; {@code false} otherwise.
     */
    boolean isFraudDataCollectionEnabled() {
        return cardConfiguration.isFraudDataCollectionEnabled();
    }

    /**
     * @return {@code true} if Venmo is enabled for the merchant account; {@code false} otherwise.
     */
    public boolean isVenmoEnabled() {
        return venmoConfiguration.isAccessTokenValid();
    }

    /**
     * @return the Access Token used by the Venmo app to tokenize on behalf of the merchant.
     */
    String getVenmoAccessToken() {
        return venmoConfiguration.getAccessToken();
    }

    /**
     * @return the Venmo merchant id used by the Venmo app to authorize payment.
     */
    String getVenmoMerchantId() {
        return venmoConfiguration.getMerchantId();
    }

    /**
     * @return the Venmo environment used to handle this payment.
     */
    String getVenmoEnvironment() {
        return venmoConfiguration.getEnvironment();
    }

    /**
     * @return {@code true} if GraphQL is enabled for the merchant account; {@code false} otherwise.
     */
    boolean isGraphQLEnabled() {
        return graphQLConfiguration.isEnabled();
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
        return kountConfiguration.isEnabled();
    }

    /**
     * @return the Kount merchant id set in the Gateway.
     */
    String getKountMerchantId() {
        return kountConfiguration.getKountMerchantId();
    }

    /**
     * @return {@code true} if UnionPay is enabled for the merchant account; {@code false} otherwise.
     */
    public boolean isUnionPayEnabled() {
        return unionPayConfiguration.isEnabled();
    }

    /**
     * @return The current environment.
     */
    @NonNull
    public String getEnvironment() {
        return environment;
    }

    /**
     * @return {@code true} if PayPal is enabled and supported in the current environment,
     *         {@code false} otherwise.
     */
    public boolean isPayPalEnabled() {
        return paypalEnabled;
    }

    /**
     * @return the PayPal app display name.
     */
    String getPayPalDisplayName() {
        return payPalConfiguration.getDisplayName();
    }

    /**
     * @return the PayPal app client id.
     */
    String getPayPalClientId() {
        return payPalConfiguration.getClientId();
    }

    /**
     * @return the PayPal app privacy url.
     */
    @Nullable
    public String getPayPalPrivacyUrl() {
        return payPalConfiguration.getPrivacyUrl();
    }

    /**
     * @return the PayPal app user agreement url.
     */
    @Nullable
    public String getPayPalUserAgreementUrl() {
        return payPalConfiguration.getUserAgreementUrl();
    }

    /**
     * @return the url for custom PayPal environments.
     */
    @Nullable
    public String getPayPalDirectBaseUrl() {
        return payPalConfiguration.getDirectBaseUrl();
    }

    /**
     * @return the current environment for PayPal.
     */
    String getPayPalEnvironment() {
        return payPalConfiguration.getEnvironment();
    }

    /**
     * @return {@code true} if PayPal touch is currently disabled, {@code false} otherwise.
     */
    boolean isPayPalTouchDisabled() {
        return payPalConfiguration.isTouchDisabled();
    }

    /**
     * @return the PayPal currency code.
     */
    String getPayPalCurrencyIsoCode() {
        return payPalConfiguration.getCurrencyIsoCode();
    }

    /**
     * @return {@code true} if Google Payment is enabled and supported in the current environment; {@code false} otherwise.
     */
    public boolean isGooglePayEnabled() {
        return googlePayConfiguration.isEnabled();
    }

    /**
     * @return the authorization fingerprint to use for Google Payment, only allows tokenizing Google Payment cards.
     */
    String getGooglePayAuthorizationFingerprint() {
        return googlePayConfiguration.getGoogleAuthorizationFingerprint();
    }

    /**
     * @return the current Google Pay environment.
     */
    String getGooglePayEnvironment() {
        return googlePayConfiguration.getEnvironment();
    }

    /**
     * @return the Google Pay display name to show to the user.
     */
    String getGooglePayDisplayName() {
        return googlePayConfiguration.getDisplayName();
    }

    /**
     * @return a list of supported card networks for Google Pay.
     */
    List<String> getGooglePaySupportedNetworks() {
        return googlePayConfiguration.getSupportedNetworks();
    }

    /**
     * @return the PayPal Client ID used by Google Pay.
     */
    String getGooglePayPayPalClientId() {
        return googlePayConfiguration.getPaypalClientId();
    }

    /**
     * @return {@code true} if 3D Secure is enabled and supported for the current merchant account,
     *         {@code false} otherwise.
     */
    public boolean isThreeDSecureEnabled() {
        return threeDSecureEnabled;
    }

    /**
     * @return the current Braintree merchant id.
     */
    @NonNull
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * @return the current Braintree merchant account id.
     */
    @Nullable
    public String getMerchantAccountId() {
        return merchantAccountId;
    }

    /**
     * @return {@link String} url of the Braintree analytics service.
     */
    String getAnalyticsUrl() {
        return analyticsConfiguration.getUrl();
    }

    /**
     * @return {@code true} if analytics are enabled, {@code false} otherwise.
     */
    boolean isAnalyticsEnabled() {
        return analyticsConfiguration.isEnabled();
    }

    /**
     * @return {@code true} if Visa Checkout is enabled for the merchant account; {@code false} otherwise.
     */
    public boolean isVisaCheckoutEnabled() {
        return visaCheckoutConfiguration.isEnabled();
    }

    /**
     * @return the Visa Checkout supported networks enabled for the merchant account.
     */
    List<String> getVisaCheckoutSupportedNetworks() {
        return visaCheckoutConfiguration.getAcceptedCardBrands();
    }

    /**
     * @return the Visa Checkout API key configured in the Braintree Control Panel.
     */
    String getVisaCheckoutApiKey() {
        return visaCheckoutConfiguration.getApiKey();
    }

    /**
     * @return the Visa Checkout External Client ID configured in the Braintree Control Panel.
     */
    String getVisaCheckoutExternalClientId() {
        return visaCheckoutConfiguration.getExternalClientId();
    }

    /**
     * Check if a specific feature is enabled in the GraphQL API.
     *
     * @param feature The feature to check.
     * @return {@code true} if GraphQL is enabled and the feature is enabled, {@code false} otherwise.
     */
    boolean isGraphQLFeatureEnabled(String feature) {
        return graphQLConfiguration.isFeatureEnabled(feature);
    }

    /**
     * @return the GraphQL url.
     */
    String getGraphQLUrl() {
        return graphQLConfiguration.getUrl();
    }

    /**
     * @return {@code true} if Samsung Pay is enabled; {@code false} otherwise.
     */
    public boolean isSamsungPayEnabled() {
        return samsungPayConfiguration.isEnabled();
    }

    /**
     * @return the merchant display name for Samsung Pay.
     */
    String getSamsungPayMerchantDisplayName() {
        return samsungPayConfiguration.getMerchantDisplayName();
    }

    /**
     * @return the Samsung Pay service id associated with the merchant.
     */
    String getSamsungPayServiceId() {
        return samsungPayConfiguration.getServiceId();
    }

    /**
     * @return a list of card brands supported by Samsung Pay.
     */
    List<String> getSamsungPaySupportedCardBrands() {
        return new ArrayList<>(samsungPayConfiguration.getSupportedCardBrands());
    }

    /**
     * @return the authorization to use with Samsung Pay.
     */
    String getSamsungPayAuthorization() {
        return samsungPayConfiguration.getSamsungAuthorization();
    }

    /**
     * @return the Braintree environment Samsung Pay should interact with.
     */
    String getSamsungPayEnvironment() {
        return samsungPayConfiguration.getEnvironment();
    }

    private void parseJsonChallenges(JSONArray jsonArray) {
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                challenges.add(jsonArray.optString(i, ""));
            }
        }
    }

    /**
     * @return the JWT for Cardinal
     */
    @Nullable
    public String getCardinalAuthenticationJwt() {
        return cardinalAuthenticationJwt;
    }

    /**
     * @return The Access Token for Braintree API.
     */
    String getBraintreeApiAccessToken() {
        return braintreeApiConfiguration.getAccessToken();
    }

    /**
     * @return the base url for accessing Braintree API.
     */
    String getBraintreeApiUrl() {
        return braintreeApiConfiguration.getUrl();
    }

    /**
     * @return a boolean indicating whether Braintree API is enabled for this merchant.
     */
    boolean isBraintreeApiEnabled() {
        return braintreeApiConfiguration.isEnabled();
    }

    /**
     * @return a {@link List<String>} of card types supported by the merchant.
     */
    List<String> getSupportedCardTypes() {
        return cardConfiguration.getSupportedCardTypes();
    }
}
