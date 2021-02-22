package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;


import java.util.concurrent.TimeUnit;

class ConfigurationCache {

    private static final long TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5);

    private ConfigurationCache() {}

    static String getConfiguration(Context context, String cacheKey) {
        return getConfiguration(context, cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    static String getConfiguration(Context context, String cacheKey, long currentTimeMillis) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(context);

        String timestampKey = cacheKey + "_timestamp";
        if (prefs.contains(timestampKey)) {
            long timeInCache = (currentTimeMillis - prefs.getLong(timestampKey, 0));
            if (timeInCache < TIME_TO_LIVE) {
                return prefs.getString(cacheKey, "");
            }
        }
        return null;
    }

    static void saveConfiguration(Context context, Configuration configuration, String cacheKey) {
        saveConfiguration(context, configuration, cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    static void saveConfiguration(Context context, Configuration configuration, String cacheKey, long currentTimeMillis) {
        String timestampKey = String.format("%s_timestamp", cacheKey);
        BraintreeSharedPreferences.getSharedPreferences(context).edit()
                .putString(cacheKey, configuration.toJson())
                .putLong(timestampKey, currentTimeMillis)
                .apply();
    }
}
