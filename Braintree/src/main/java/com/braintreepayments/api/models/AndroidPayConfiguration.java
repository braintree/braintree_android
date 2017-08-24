package com.braintreepayments.api.models;

import android.content.Context;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Json;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.Wallet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains the remote Android Pay configuration for the Braintree SDK.
 */
public class AndroidPayConfiguration {

    private static final String ENABLED_KEY = "enabled";
    private static final String GOOGLE_AUTHORIZATION_FINGERPRINT_KEY = "googleAuthorizationFingerprint";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String SUPPORTED_NETWORKS_KEY = "supportedNetworks";

    private boolean mEnabled;
    private String mGoogleAuthorizationFingerprint;
    private String mEnvironment;
    private String mDisplayName;
    private String[] mSupportedNetworks;

    /**
     * Parse an {@link AndroidPayConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link AndroidPayConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    public static AndroidPayConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        AndroidPayConfiguration androidPayConfiguration = new AndroidPayConfiguration();
        androidPayConfiguration.mEnabled = json.optBoolean(ENABLED_KEY, false);
        androidPayConfiguration.mGoogleAuthorizationFingerprint = Json.optString(json,
                GOOGLE_AUTHORIZATION_FINGERPRINT_KEY, null);
        androidPayConfiguration.mEnvironment = Json.optString(json, ENVIRONMENT_KEY, null);
        androidPayConfiguration.mDisplayName = Json.optString(json, DISPLAY_NAME_KEY, "");

        JSONArray supportedNetworks = json.optJSONArray(SUPPORTED_NETWORKS_KEY);
        if (supportedNetworks != null) {
            androidPayConfiguration.mSupportedNetworks = new String[supportedNetworks.length()];
            for (int i = 0; i < supportedNetworks.length(); i++) {
                try {
                    androidPayConfiguration.mSupportedNetworks[i] = supportedNetworks.getString(i);
                } catch (JSONException ignored) {}
            }
        } else {
            androidPayConfiguration.mSupportedNetworks = new String[0];
        }

        return androidPayConfiguration;
    }

    /**
     * @return {@code true} if Android Pay is enabled and supported in the current environment,
     *         {@code false} otherwise. Note: this value only pertains to the Braintree configuration, to check if
     *         the user has Android Pay setup use
     *         {@link com.braintreepayments.api.AndroidPay#isReadyToPay(BraintreeFragment, BraintreeResponseListener)}
     */
    public boolean isEnabled(Context context) {
        try {
            Class.forName(Wallet.class.getName());

            return mEnabled && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                    ConnectionResult.SUCCESS;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * @return the authorization fingerprint to use for Android Pay, only allows tokenizing Android Pay cards.
     */
    public String getGoogleAuthorizationFingerprint() {
        return mGoogleAuthorizationFingerprint;
    }

    /**
     * @return the current Android Pay environment.
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
     * @return a string array of supported card networks for Android Pay.
     */
    public String[] getSupportedNetworks() {
        return mSupportedNetworks;
    }
}
