package com.braintreepayments.api.models;

/**
 * Contains the remote PayPal configuration for the Braintree SDK.
 */
public class PayPalConfiguration {

    private String displayName;
    private String clientId;
    private String privacyUrl;
    private String userAgreementUrl;
    private String directBaseUrl;
    private String environment;
    private boolean touchDisabled;

    /**
     * @return the PayPal app display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the PayPal app client id.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the PayPal app privacy url.
     */
    public String getPrivacyUrl() {
        return privacyUrl;
    }

    /**
     * @return the PayPal app user agreement url.
     */
    public String getUserAgreementUrl() {
        return userAgreementUrl;
    }

    /**
     * @return the url for custom PayPal environments.
     */
    public String getDirectBaseUrl() {
        return directBaseUrl + "/v1/";
    }

    /**
     * @return the current environment for PayPal.
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * @return {@code true} if PayPal touch is currently disabled, {@code false} otherwise.
     */
    public boolean getTouchDisabled() {
        return touchDisabled;
    }
}
