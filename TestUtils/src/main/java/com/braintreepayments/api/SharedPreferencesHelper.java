package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SharedPreferencesHelper {

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPreferences(Context context, String fileName) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public static void overrideConfigurationCache(Context context, Authorization authorization, Configuration configuration) {
        final String configUrl = Uri.parse(authorization.getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        String cacheKey = Base64.encodeToString(String.format("%s%s", configUrl, authorization.getBearer()).getBytes(), 0);
        String timestampKey = String.format("%s_timestamp", cacheKey);
        try {
            new BraintreeSharedPreferences()
                    .getSharedPreferences(context)
                    .edit()
                    .putString(cacheKey, configuration.toJson())
                    .putLong(timestampKey, System.currentTimeMillis())
                    .apply();
        } catch (GeneralSecurityException | IOException ignored) {
        }
    }

    public static void clearConfigurationCacheOverride(Context context) {
        try {
           new BraintreeSharedPreferences()
                    .getSharedPreferences(context)
                    .edit()
                    .clear()
                    .apply();
        } catch (GeneralSecurityException | IOException ignored) {
        }
    }

    @SuppressWarnings("ApplySharedPref")
    public static void clearSharedPreferences(Context context) {
        getSharedPreferences(context).edit().clear().commit();
    }

    public static void writeMockConfiguration(Context context, String configUrl, String appendedAuthorization,
            String configurationString) {
        writeMockConfiguration(context, configUrl, appendedAuthorization, configurationString,
                System.currentTimeMillis());
    }

    @SuppressWarnings("ApplySharedPref")
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
