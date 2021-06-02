package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Contains the remote PayPal configuration for the Braintree SDK.
 */
class PayPalConfiguration {

    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String PRIVACY_URL_KEY = "privacyUrl";
    private static final String USER_AGREEMENT_URL_KEY = "userAgreementUrl";
    private static final String DIRECT_BASE_URL_KEY = "directBaseUrl";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String TOUCH_DISABLED_KEY = "touchDisabled";
    private static final String CURRENCY_ISO_CODE_KEY = "currencyIsoCode";

    private String displayName;
    private String clientId;
    private String privacyUrl;
    private String userAgreementUrl;
    private String directBaseUrl;
    private String environment;
    private boolean touchDisabled;
    private String currencyIsoCode;

    /**
     * Parse an {@link PayPalConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link PayPalConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    static PayPalConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        PayPalConfiguration payPalConfiguration = new PayPalConfiguration();
        payPalConfiguration.displayName = Json.optString(json, DISPLAY_NAME_KEY, "");
        payPalConfiguration.clientId = Json.optString(json, CLIENT_ID_KEY, "");
        payPalConfiguration.privacyUrl = Json.optString(json, PRIVACY_URL_KEY, "");
        payPalConfiguration.userAgreementUrl = Json.optString(json, USER_AGREEMENT_URL_KEY, "");
        payPalConfiguration.directBaseUrl = Json.optString(json, DIRECT_BASE_URL_KEY, "");
        payPalConfiguration.environment = Json.optString(json, ENVIRONMENT_KEY, "");
        payPalConfiguration.touchDisabled = json.optBoolean(TOUCH_DISABLED_KEY, true);
        payPalConfiguration.currencyIsoCode = Json.optString(json, CURRENCY_ISO_CODE_KEY, "");

        return payPalConfiguration;
    }

    /**
     * @return the PayPal app display name.
     */
    String getDisplayName() {
        return displayName;
    }

    /**
     * @return the PayPal app client id.
     */
    String getClientId() {
        return clientId;
    }

    /**
     * @return the PayPal app privacy url.
     */
    String getPrivacyUrl() {
        return privacyUrl;
    }

    /**
     * @return the PayPal app user agreement url.
     */
    String getUserAgreementUrl() {
        return userAgreementUrl;
    }

    /**
     * @return the url for custom PayPal environments.
     */
    String getDirectBaseUrl() {
        return (TextUtils.isEmpty(directBaseUrl) ? "" : directBaseUrl + "/v1/");
    }

    /**
     * @return the current environment for PayPal.
     */
    String getEnvironment() {
        return environment;
    }

    /**
     * @return {@code true} if PayPal touch is currently disabled, {@code false} otherwise.
     */
    boolean isTouchDisabled() {
        return touchDisabled;
    }

    /**
     * @return the PayPal currency code.
     */
    String getCurrencyIsoCode() {
        return currencyIsoCode;
    }
}
