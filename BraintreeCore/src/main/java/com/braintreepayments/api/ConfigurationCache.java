package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

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

    String getConfiguration(Context context, String cacheKey) throws UnexpectedException {
        BraintreeSharedPreferences sharedPreferences =
                BraintreeSharedPreferences.getInstance(context);
        return getConfiguration(sharedPreferences, cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    String getConfiguration(BraintreeSharedPreferences sharedPreferences, String cacheKey, long currentTimeMillis) throws UnexpectedException {
        String timestampKey = cacheKey + "_timestamp";
        if (sharedPreferences.containsKey(timestampKey)) {
            long timeInCache = (currentTimeMillis - sharedPreferences.getLong(timestampKey));
            if (timeInCache < TIME_TO_LIVE) {
                return sharedPreferences.getString(cacheKey, "");
            }
        }
        return null;
    }

    void saveConfiguration(Context context, Configuration configuration, String cacheKey) throws UnexpectedException {
        BraintreeSharedPreferences sharedPreferences =
            BraintreeSharedPreferences.getInstance(context);
        saveConfiguration(sharedPreferences, configuration, cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    void saveConfiguration(BraintreeSharedPreferences sharedPreferences, Configuration configuration, String cacheKey, long currentTimeMillis) throws UnexpectedException {
        String timestampKey = String.format("%s_timestamp", cacheKey);
        sharedPreferences.putStringAndLong(cacheKey, configuration.toJson(), timestampKey, currentTimeMillis);
    }
}
