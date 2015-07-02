package com.braintreepayments.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Contains the remote PayPal configuration for the Braintree SDK.
 */
public class PayPalConfiguration {

    @SerializedName("displayName") private String mDisplayName;
    @SerializedName("clientId") private String mClientId;
    @SerializedName("privacyUrl") private String mPrivacyUrl;
    @SerializedName("userAgreementUrl") private String mUserAgreementUrl;
    @SerializedName("directBaseUrl") private String mDirectBaseUrl;
    @SerializedName("environment") private String mEnvironment;
    @SerializedName("touchDisabled") private boolean mTouchDisabled;
    @SerializedName("currencyIsoCode") private String mCurrencyIsoCode;

    /**
     * @return the PayPal app display name.
     */
    public String getDisplayName() {
        return null;
    }

    /**
     * @return the PayPal app client id.
     */
    public String getClientId() {
        return null;
    }

    /**
     * @return the PayPal app privacy url.
     */
    public String getPrivacyUrl() {
        return null;
    }

    /**
     * @return the PayPal app user agreement url.
     */
    public String getUserAgreementUrl() {
        return null;
    }

    /**
     * @return the url for custom PayPal environments.
     */
    public String getDirectBaseUrl() {
        return null;
    }

    /**
     * @return the current environment for PayPal.
     */
    public String getEnvironment() {
        return null;
    }

    /**
     * @return {@code true} if PayPal touch is currently disabled, {@code false} otherwise.
     */
    public boolean getTouchDisabled() {
        return mTouchDisabled;
    }

    /**
     * @return the PayPal currency code.
     */
    public String getCurrencyIsoCode() { return null; }
}
