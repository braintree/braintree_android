package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

class ConfigurationCache {

    private static final long TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5);

    private static volatile ConfigurationCache INSTANCE;

    static ConfigurationCache getInstance() {
        if (INSTANCE == null) {
            synchronized (ConfigurationCache.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    INSTANCE = new ConfigurationCache();
                }
            }
        }
        return INSTANCE;
    }

    @VisibleForTesting
    ConfigurationCache() {}

    String getConfiguration(Context context, String cacheKey) {
        return getConfiguration(context, new BraintreeSharedPreferences(), cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    String getConfiguration(Context context, BraintreeSharedPreferences braintreeSharedPreferences, String cacheKey, long currentTimeMillis) {
        SharedPreferences prefs;
        try {
            prefs = braintreeSharedPreferences.getSharedPreferences(context);
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }

        String timestampKey = cacheKey + "_timestamp";
        if (prefs.contains(timestampKey)) {
            long timeInCache = (currentTimeMillis - prefs.getLong(timestampKey, 0));
            if (timeInCache < TIME_TO_LIVE) {
                return prefs.getString(cacheKey, "");
            }
        }
        return null;
    }

    void saveConfiguration(Context context, Configuration configuration, String cacheKey) {
        saveConfiguration(context, configuration, new BraintreeSharedPreferences(), cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    void saveConfiguration(Context context, Configuration configuration, BraintreeSharedPreferences braintreeSharedPreferences, String cacheKey, long currentTimeMillis) {
        String timestampKey = String.format("%s_timestamp", cacheKey);
        try {
            braintreeSharedPreferences.getSharedPreferences(context).edit()
                    .putString(cacheKey, configuration.toJson())
                    .putLong(timestampKey, currentTimeMillis)
                    .apply();
        } catch (GeneralSecurityException | IOException ignored) {
        }
    }
}
