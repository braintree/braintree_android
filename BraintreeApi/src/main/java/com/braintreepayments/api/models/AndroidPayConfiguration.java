package com.braintreepayments.api.models;

import android.content.Context;

import com.braintreepayments.api.annotations.Beta;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONObject;

/**
 * Contains the remote Android Pay configuration for the Braintree SDK.
 */
@Beta
public class AndroidPayConfiguration {

    private static final String ENABLED_KEY = "enabled";
    private static final String GOOGLE_AUTHORIZATION_FINGERPRINT_KEY = "googleAuthorizationFingerprint";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String DISPLAY_NAME_KEY = "displayName";

    private boolean mEnabled;
    private String mGoogleAuthorizationFingerprint;
    private String mEnvironment;
    private String mDisplayName;

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
        androidPayConfiguration.mGoogleAuthorizationFingerprint = json.optString(GOOGLE_AUTHORIZATION_FINGERPRINT_KEY, null);
        androidPayConfiguration.mEnvironment = json.optString(ENVIRONMENT_KEY, null);
        androidPayConfiguration.mDisplayName = json.optString(DISPLAY_NAME_KEY, null);

        return androidPayConfiguration;
    }

    /**
     * @return {@code true} if Android Pay is enabled and supported in the current environment,
     *         {@code false} otherwise.
     */
    public boolean isEnabled(Context context) {
        try {
            return mEnabled &&
                    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                            ConnectionResult.SUCCESS;
        } catch (Exception e) {
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
}
