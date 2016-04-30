package com.braintreepayments.api.models;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.braintreepayments.api.Venmo;

import org.json.JSONObject;

/**
 * Contains the remote Pay with Venmo configuration for the Braintree SDK.
 */
public class VenmoConfiguration {

    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final Uri VENMO_AUTHORITY_URI =
            Uri.parse("content://com.venmo.whitelistprovider");

    private String mAccessToken;

    /**
     * Parses the Pay with Venmo configuration from json.
     *
     * @param json The json to parse.
     * @return A {@link VenmoConfiguration} instance with data that was able to be parsed from the
     * {@link JSONObject}.
     */
    static VenmoConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        VenmoConfiguration venmoConfiguration = new VenmoConfiguration();
        venmoConfiguration.mAccessToken = json.optString(ACCESS_TOKEN_KEY, "");

        return venmoConfiguration;
    }

    /**
     * @return The access token to use Pay with Venmo
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Determines if the Pay with Venmo flow is available to be used. This can be used to determine
     * if UI components should be shown or hidden.
     *
     * @param context A context to access the {@link PackageManager}
     * @return boolean if Venmo is enabled, and available to be used
     */
    public boolean isEnabled(Context context) {
        return isAccessTokenValid() &&
                isVenmoWhitelisted(context.getContentResolver()) &&
                Venmo.isVenmoInstalled(context);
    }

    public boolean isAccessTokenValid() {
        return !TextUtils.isEmpty(mAccessToken);
    }

    public boolean isVenmoWhitelisted(ContentResolver contentResolver) {
        Cursor cursor = contentResolver.query(VENMO_AUTHORITY_URI, null, null, null, null);

        boolean isVenmoWhiteListed = cursor != null && cursor.moveToFirst() && "true".equals(cursor.getString(0));

        if (cursor != null) {
            cursor.close();
        }

        return isVenmoWhiteListed;
    }
}
