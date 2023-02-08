package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.sharedutils.R;

class BraintreeSharedPreferences {

    private static volatile BraintreeSharedPreferences INSTANCE;

    static BraintreeSharedPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BraintreeSharedPreferences.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    INSTANCE = new BraintreeSharedPreferences(context);
                }
            }
        }
        return INSTANCE;
    }

    private static SharedPreferences createSharedPreferencesInstance(Context context) {
        String preferenceFileKey = context.getString(R.string.preference_file_key);
        return context.getSharedPreferences(preferenceFileKey, Context.MODE_PRIVATE);
    }

    private final SharedPreferences sharedPreferences;

    BraintreeSharedPreferences(Context context) {
        this(createSharedPreferencesInstance(context));
    }

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
