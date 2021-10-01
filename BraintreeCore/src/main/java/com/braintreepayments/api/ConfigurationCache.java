package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;

class ConfigurationCache {

    private static final long TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5);

    private static volatile ConfigurationCache INSTANCE;
    private final BraintreeSharedPreferences braintreeSharedPreferences;

    static ConfigurationCache getInstance() {
        if (INSTANCE == null) {
            synchronized (ConfigurationCache.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    INSTANCE = new ConfigurationCache(BraintreeSharedPreferences.getInstance());
                }
            }
        }
        return INSTANCE;
    }

    @VisibleForTesting
    ConfigurationCache(BraintreeSharedPreferences braintreeSharedPreferences) {
        this.braintreeSharedPreferences = braintreeSharedPreferences;
    }

    String getConfiguration(Context context, String cacheKey) {
        return getConfiguration(context, cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    String getConfiguration(Context context, String cacheKey, long currentTimeMillis) {
        String timestampKey = cacheKey + "_timestamp";
        if (braintreeSharedPreferences.containsKey(context, timestampKey)) {
            long timeInCache = (currentTimeMillis - braintreeSharedPreferences.getLong(context, timestampKey));
            if (timeInCache < TIME_TO_LIVE) {
                return braintreeSharedPreferences.getString(context, cacheKey);
            }
        }

        return null;
    }

    void saveConfiguration(Context context, Configuration configuration, String cacheKey) {
        saveConfiguration(context, configuration, cacheKey, System.currentTimeMillis());
    }

    @VisibleForTesting
    void saveConfiguration(Context context, Configuration configuration, String cacheKey, long currentTimeMillis) {
        String timestampKey = String.format("%s_timestamp", cacheKey);
        braintreeSharedPreferences.putStringAndLong(context, cacheKey, configuration.toJson(), timestampKey, currentTimeMillis);
    }
}
