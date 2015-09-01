package com.braintreepayments.api.models;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Contains the remote PayPal configuration for the Braintree SDK.
 */
public class PayPalConfiguration {

    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String PRIVACY_URL_KEY = "privacyUrl";
    private static final String USER_AGREEMENT_URL_KEY = "userAgreementUrl";
    private static final String DIRECT_BASE_URL_KEY = "directBaseUrl";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String TOUCH_DISABLED_KEY = "touchDisabled";
    private static final String CURRENCY_ISO_CODE_KEY = "currencyIsoCode";
    private static final String BILLING_AGREEMENT_KEY = "billingAgreementsEnabled";

    private String mDisplayName;
    private String mClientId;
    private String mPrivacyUrl;
    private String mUserAgreementUrl;
    private String mDirectBaseUrl;
    private String mEnvironment;
    private boolean mTouchDisabled;
    private String mCurrencyIsoCode;
    private boolean mUseBillingAgreement;

    /**
     * Parse an {@link PayPalConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link PayPalConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    public static PayPalConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        PayPalConfiguration payPalConfiguration = new PayPalConfiguration();
        payPalConfiguration.mDisplayName = json.optString(DISPLAY_NAME_KEY, null);
        payPalConfiguration.mClientId = json.optString(CLIENT_ID_KEY, null);
        payPalConfiguration.mPrivacyUrl = json.optString(PRIVACY_URL_KEY, null);
        payPalConfiguration.mUserAgreementUrl = json.optString(USER_AGREEMENT_URL_KEY, null);
        payPalConfiguration.mDirectBaseUrl = json.optString(DIRECT_BASE_URL_KEY, null);
        payPalConfiguration.mEnvironment = json.optString(ENVIRONMENT_KEY, null);
        payPalConfiguration.mTouchDisabled = json.optBoolean(TOUCH_DISABLED_KEY, true);
        payPalConfiguration.mCurrencyIsoCode = json.optString(CURRENCY_ISO_CODE_KEY, null);
        payPalConfiguration.mUseBillingAgreement = json.optBoolean(BILLING_AGREEMENT_KEY, false);

        return payPalConfiguration;
    }

    /**
     * @return {@code true} if PayPal is enabled, {@code false} otherwise.
     */
    public boolean isEnabled() {
        return (!TextUtils.isEmpty(mDisplayName) && !TextUtils.isEmpty(mClientId) &&
                !TextUtils.isEmpty(mPrivacyUrl) && !TextUtils.isEmpty(mUserAgreementUrl));
    }

    /**
     * @return the PayPal app display name.
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * @return the PayPal app client id.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * @return the PayPal app privacy url.
     */
    public String getPrivacyUrl() {
        return mPrivacyUrl;
    }

    /**
     * @return the PayPal app user agreement url.
     */
    public String getUserAgreementUrl() {
        return mUserAgreementUrl;
    }

    /**
     * @return the url for custom PayPal environments.
     */
    public String getDirectBaseUrl() {
        return (TextUtils.isEmpty(mDirectBaseUrl) ? null : mDirectBaseUrl + "/v1/");
    }

    /**
     * @return the current environment for PayPal.
     */
    public String getEnvironment() {
        return mEnvironment;
    }

    /**
     * @return {@code true} if PayPal touch is currently disabled, {@code false} otherwise.
     */
    public boolean isTouchDisabled() {
        return mTouchDisabled;
    }

    /**
     * @return the PayPal currency code.
     */
    public String getCurrencyIsoCode() {
        return mCurrencyIsoCode;
    }

    /**
     * @return if billing agreements are enabled.
     */
    public boolean getUseBillingAgreement() {
        return mUseBillingAgreement;
    }
}
