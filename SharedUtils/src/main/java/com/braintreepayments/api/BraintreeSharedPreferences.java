package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

class BraintreeSharedPreferences {

    private static final String PREFERENCES_FILE_KEY =
        "com.braintreepayments.api.SHARED_PREFERENCES";
    private static volatile BraintreeSharedPreferences INSTANCE;

    static BraintreeSharedPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BraintreeSharedPreferences.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    INSTANCE =
                        new BraintreeSharedPreferences(createSharedPreferencesInstance(context));
                }
            }
        }
        return INSTANCE;
    }

    private static SharedPreferences createSharedPreferencesInstance(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE);
    }

    private final SharedPreferences sharedPreferences;

    @VisibleForTesting
    BraintreeSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    String getString(String key, String fallback) {
        return sharedPreferences.getString(key, fallback);
    }

    void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    boolean containsKey(String key) {
        return sharedPreferences.contains(key);
    }

    long getLong(String key) {
        return sharedPreferences.getLong(key, 0);
    }

    void putStringAndLong(String stringKey, String stringValue, String longKey, long longValue) {
        sharedPreferences
                .edit()
                .putString(stringKey, stringValue)
                .putLong(longKey, longValue)
                .apply();
    }

    void clearSharedPreferences() {
        sharedPreferences.edit().clear().apply();
    }
}
