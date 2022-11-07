package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;

class ConfigurationCache {

    private static final long TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5);

    private static volatile ConfigurationCache INSTANCE;

    private final BraintreeSharedPreferences sharedPreferences;

    static ConfigurationCache getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ConfigurationCache.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    INSTANCE = new ConfigurationCache(BraintreeSharedPreferences.getInstance(context));
                }
            }
        }
        return INSTANCE;
    }

    @VisibleForTesting
    ConfigurationCache(BraintreeSharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    String getConfiguration(String cacheKey) throws BraintreeSharedPreferencesException {
        return getConfiguration(cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    String getConfiguration(String cacheKey, long currentTimeMillis) throws BraintreeSharedPreferencesException {
        String timestampKey = cacheKey + "_timestamp";
        if (sharedPreferences.containsKey(timestampKey)) {
            long timeInCache = (currentTimeMillis - sharedPreferences.getLong(timestampKey));
            if (timeInCache < TIME_TO_LIVE) {
                return sharedPreferences.getString(cacheKey, "");
            }
        }
        return null;
    }

    void saveConfiguration(Configuration configuration, String cacheKey) throws BraintreeSharedPreferencesException {
        saveConfiguration(configuration, cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    void saveConfiguration(Configuration configuration, String cacheKey, long currentTimeMillis) throws BraintreeSharedPreferencesException {
        String timestampKey = String.format("%s_timestamp", cacheKey);
        sharedPreferences.putStringAndLong(cacheKey, configuration.toJson(), timestampKey, currentTimeMillis);
    }
}
