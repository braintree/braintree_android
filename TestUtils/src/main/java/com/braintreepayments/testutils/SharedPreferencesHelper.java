package com.braintreepayments.testutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

public class SharedPreferencesHelper {

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
    }

    public static void clearSharedPreferences(Context context) {
        getSharedPreferences(context).edit().clear().commit();
    }

    public static void writeMockConfiguration(Context context, String configUrl, String configurationString) {
        writeMockConfiguration(context, configUrl, configurationString, System.currentTimeMillis());
    }

    public static void writeMockConfiguration(Context context, String configUrl, String configurationString,
            long timestamp) {
        writeMockConfiguration(context, configUrl, null, configurationString, timestamp);
    }

    public static void writeMockConfiguration(Context context, String configUrl, String appendedAuthorization,
            String configurationString, long timestamp) {
        configUrl = Uri.parse(configUrl)
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        if (appendedAuthorization != null) {
            configUrl = configUrl.concat(appendedAuthorization);
        }

        String key = Base64.encodeToString(configUrl.getBytes(), 0);
        getSharedPreferences(context).edit()
                .putString(key, configurationString)
                .putLong(key + "_timestamp", timestamp)
                .commit();
    }
}
