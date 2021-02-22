package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the remote Google Pay configuration for the Braintree SDK.
 */
class GooglePayConfiguration {
    private static final String ENABLED_KEY = "enabled";
    private static final String GOOGLE_AUTHORIZATION_FINGERPRINT_KEY = "googleAuthorizationFingerprint";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String SUPPORTED_NETWORKS_KEY = "supportedNetworks";
    private static final String PAYPAL_CLIENT_ID_KEY = "paypalClientId";

    private boolean mEnabled;
    private String mGoogleAuthorizationFingerprint;
    private String mEnvironment;
    private String mDisplayName;
    private List<String> mSupportedNetworks;
    private String mPayPalClientId;

    /**
     * Parse an {@link GooglePayConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link GooglePayConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    static GooglePayConfiguration fromJson(JSONObject json) {
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
            googlePayConfiguration.mSupportedNetworks = new ArrayList<>();
            for (int i = 0; i < supportedNetworks.length(); i++) {
                try {
                    googlePayConfiguration.mSupportedNetworks.add(supportedNetworks.getString(i));
                } catch (JSONException ignored) {}
            }
        } else {
            googlePayConfiguration.mSupportedNetworks = new ArrayList<>();
        }

        return googlePayConfiguration;
    }

    /**
     * @return {@code true} if Google Pay is enabled and supported in the current environment,
     *         {@code false} otherwise.
     */
    boolean isEnabled() {
        return mEnabled;
    }

    /**
     * @return the authorization fingerprint to use for Google Pay, only allows tokenizing Google Pay cards.
     */
    String getGoogleAuthorizationFingerprint() {
        return mGoogleAuthorizationFingerprint;
    }

    /**
     * @return the current Google Pay environment.
     */
    String getEnvironment() {
        return mEnvironment;
    }

    /**
     * @return the display name to show to the user.
     */
    String getDisplayName() {
        return mDisplayName;
    }

    /**
     * @return a string array of supported card networks for Google Pay.
     */
    List<String> getSupportedNetworks() {
        return Collections.unmodifiableList(mSupportedNetworks);
    }

    /**
     * @return the PayPal Client ID.
     */
    String getPaypalClientId() {
        return mPayPalClientId;
    }
}
