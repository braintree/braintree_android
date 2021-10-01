package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

class BraintreeSharedPreferences {

    private static volatile BraintreeSharedPreferences INSTANCE;
    private static final String BRAINTREE_SHARED_PREFS_FILENAME = "BraintreeApi";

    static BraintreeSharedPreferences getInstance() {
        if (INSTANCE == null) {
            synchronized (BraintreeSharedPreferences.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    INSTANCE = new BraintreeSharedPreferences();
                }
            }
        }
        return INSTANCE;
    }

    static SharedPreferences getSharedPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    BRAINTREE_SHARED_PREFS_FILENAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }
    }

    String getString(Context context, String key)  {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, "");
        }
        return "";
    }

    void putString(Context context, String key, String value){
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            sharedPreferences
                    .edit()
                    .putString(key, value)
                    .apply();
        }
    }

    boolean getBoolean(Context context, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            return sharedPreferences.getBoolean(key, false);
        }
        return false;
    }

    void putBoolean(Context context, String key, boolean value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            sharedPreferences
                    .edit()
                    .putBoolean(key, value)
                    .apply();
        }
    }

    boolean containsKey(Context context, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            return sharedPreferences.contains(key);
        }
        return false;
    }

    long getLong(Context context, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            return sharedPreferences.getLong(key, 0);
        }
        return 0;
    }

    void putStringAndLong(Context context, String stringKey, String stringValue, String longKey, long longValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            sharedPreferences
                    .edit()
                    .putString(stringKey, stringValue)
                    .putLong(longKey, longValue)
                    .apply();

        }
    }

    void clearSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            sharedPreferences
                    .edit()
                    .clear()
                    .apply();
        }
    }
}
