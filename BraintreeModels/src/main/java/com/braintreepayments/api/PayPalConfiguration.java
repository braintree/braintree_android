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

    private String mDisplayName;
    private String mClientId;
    private String mPrivacyUrl;
    private String mUserAgreementUrl;
    private String mDirectBaseUrl;
    private String mEnvironment;
    private boolean mTouchDisabled;
    private String mCurrencyIsoCode;

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
        payPalConfiguration.mDisplayName = Json.optString(json, DISPLAY_NAME_KEY, null);
        payPalConfiguration.mClientId = Json.optString(json, CLIENT_ID_KEY, null);
        payPalConfiguration.mPrivacyUrl = Json.optString(json, PRIVACY_URL_KEY, null);
        payPalConfiguration.mUserAgreementUrl = Json.optString(json, USER_AGREEMENT_URL_KEY, null);
        payPalConfiguration.mDirectBaseUrl = Json.optString(json, DIRECT_BASE_URL_KEY, null);
        payPalConfiguration.mEnvironment = Json.optString(json, ENVIRONMENT_KEY, null);
        payPalConfiguration.mTouchDisabled = json.optBoolean(TOUCH_DISABLED_KEY, true);
        payPalConfiguration.mCurrencyIsoCode = Json.optString(json, CURRENCY_ISO_CODE_KEY, null);

        return payPalConfiguration;
    }

    /**
     * @return the PayPal app display name.
     */
    String getDisplayName() {
        return mDisplayName;
    }

    /**
     * @return the PayPal app client id.
     */
    String getClientId() {
        return mClientId;
    }

    /**
     * @return the PayPal app privacy url.
     */
    String getPrivacyUrl() {
        return mPrivacyUrl;
    }

    /**
     * @return the PayPal app user agreement url.
     */
    String getUserAgreementUrl() {
        return mUserAgreementUrl;
    }

    /**
     * @return the url for custom PayPal environments.
     */
    String getDirectBaseUrl() {
        return (TextUtils.isEmpty(mDirectBaseUrl) ? null : mDirectBaseUrl + "/v1/");
    }

    /**
     * @return the current environment for PayPal.
     */
    String getEnvironment() {
        return mEnvironment;
    }

    /**
     * @return {@code true} if PayPal touch is currently disabled, {@code false} otherwise.
     */
    boolean isTouchDisabled() {
        return mTouchDisabled;
    }

    /**
     * @return the PayPal currency code.
     */
    String getCurrencyIsoCode() {
        return mCurrencyIsoCode;
    }
}
