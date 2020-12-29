package com.braintreepayments.testutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;

import com.braintreepayments.api.ConfigurationCache;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;

public class SharedPreferencesHelper {

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
    }

    public static void overrideConfigurationCache(Context context, Authorization authorization, Configuration configuration) {
        final String configUrl = Uri.parse(authorization.getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        String cacheKey = Base64.encodeToString(String.format("%s%s", configUrl, authorization.getBearer()).getBytes(), 0);
        String timestampKey = String.format("%s_timestamp", cacheKey);
        BraintreeSharedPreferences
                .getSharedPreferences(context)
                .edit()
                .putString(cacheKey, configuration.toJson())
                .putLong(timestampKey, System.currentTimeMillis())
                .apply();
    }

    public static void clearConfigurationCacheOverride(Context context) {
        BraintreeSharedPreferences
                .getSharedPreferences(context)
                .edit()
                .clear()
                .apply();
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
