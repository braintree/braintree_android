package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains the remote Google Payment configuration for the Braintree SDK.
 */
public class GooglePaymentConfiguration {
    private static final String ENABLED_KEY = "enabled";
    private static final String GOOGLE_AUTHORIZATION_FINGERPRINT_KEY = "googleAuthorizationFingerprint";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String SUPPORTED_NETWORKS_KEY = "supportedNetworks";
    private static final String PAYPAL_CLIENT_ID_KEY = "paypalClientId";

    boolean mEnabled;
    String mGoogleAuthorizationFingerprint;
    String mEnvironment;
    String mDisplayName;
    String[] mSupportedNetworks;
    String mPayPalClientId;

    /**
     * Parse an {@link GooglePaymentConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link GooglePaymentConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    public static GooglePaymentConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        GooglePaymentConfiguration googlePaymentConfiguration = new GooglePaymentConfiguration();
        googlePaymentConfiguration.mEnabled = json.optBoolean(ENABLED_KEY, false);
        googlePaymentConfiguration.mGoogleAuthorizationFingerprint = Json.optString(json,
                GOOGLE_AUTHORIZATION_FINGERPRINT_KEY, null);
        googlePaymentConfiguration.mEnvironment = Json.optString(json, ENVIRONMENT_KEY, null);
        googlePaymentConfiguration.mDisplayName = Json.optString(json, DISPLAY_NAME_KEY, "");
        googlePaymentConfiguration.mPayPalClientId = Json.optString(json, PAYPAL_CLIENT_ID_KEY, "");

        JSONArray supportedNetworks = json.optJSONArray(SUPPORTED_NETWORKS_KEY);
        if (supportedNetworks != null) {
            googlePaymentConfiguration.mSupportedNetworks = new String[supportedNetworks.length()];
            for (int i = 0; i < supportedNetworks.length(); i++) {
                try {
                    googlePaymentConfiguration.mSupportedNetworks[i] = supportedNetworks.getString(i);
                } catch (JSONException ignored) {}
            }
        } else {
            googlePaymentConfiguration.mSupportedNetworks = new String[0];
        }

        return googlePaymentConfiguration;
    }

    /**
     * @return {@code true} if Google Payment is enabled and supported in the current environment,
     *         {@code false} otherwise. Note: this value only pertains to the Braintree configuration, to check if
     *         the user has Google Payment setup use
     *         {@link com.braintreepayments.api.GooglePayment#isReadyToPay(BraintreeFragment, BraintreeResponseListener)}
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * @return the authorization fingerprint to use for Google Payment, only allows tokenizing Google Payment cards.
     */
    public String getGoogleAuthorizationFingerprint() {
        return mGoogleAuthorizationFingerprint;
    }

    /**
     * @return the current Google Payment environment.
     */
    public String getEnvironment() {
        return mEnvironment;
    }

    /**
     * @return the display name to show to the user.
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * @return a string array of supported card networks for Google Payment.
     */
    public String[] getSupportedNetworks() {
        return mSupportedNetworks;
    }

    /**
     * @return the PayPal Client ID.
     */
    public String getPaypalClientId() {
        return mPayPalClientId;
    }
}
