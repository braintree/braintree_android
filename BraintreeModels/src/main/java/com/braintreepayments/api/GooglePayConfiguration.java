package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains the remote Google Pay configuration for the Braintree SDK.
 */
public class GooglePayConfiguration {
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
     * Parse an {@link GooglePayConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link GooglePayConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    public static GooglePayConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        GooglePayConfiguration googlePayConfiguration = new GooglePayConfiguration();
        googlePayConfiguration.mEnabled = json.optBoolean(ENABLED_KEY, false);
        googlePayConfiguration.mGoogleAuthorizationFingerprint = Json.optString(json,
                GOOGLE_AUTHORIZATION_FINGERPRINT_KEY, null);
        googlePayConfiguration.mEnvironment = Json.optString(json, ENVIRONMENT_KEY, null);
        googlePayConfiguration.mDisplayName = Json.optString(json, DISPLAY_NAME_KEY, "");
        googlePayConfiguration.mPayPalClientId = Json.optString(json, PAYPAL_CLIENT_ID_KEY, "");

        JSONArray supportedNetworks = json.optJSONArray(SUPPORTED_NETWORKS_KEY);
        if (supportedNetworks != null) {
            googlePayConfiguration.mSupportedNetworks = new String[supportedNetworks.length()];
            for (int i = 0; i < supportedNetworks.length(); i++) {
                try {
                    googlePayConfiguration.mSupportedNetworks[i] = supportedNetworks.getString(i);
                } catch (JSONException ignored) {}
            }
        } else {
            googlePayConfiguration.mSupportedNetworks = new String[0];
        }

        return googlePayConfiguration;
    }

    /**
     * @return {@code true} if Google Pay is enabled and supported in the current environment,
     *         {@code false} otherwise.
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * @return the authorization fingerprint to use for Google Pay, only allows tokenizing Google Pay cards.
     */
    public String getGoogleAuthorizationFingerprint() {
        return mGoogleAuthorizationFingerprint;
    }

    /**
     * @return the current Google Pay environment.
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
     * @return a string array of supported card networks for Google Pay.
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
