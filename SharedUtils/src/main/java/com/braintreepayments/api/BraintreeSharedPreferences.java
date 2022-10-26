package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

// TODO: consider removing Security exception ignore statements once crypto library
// bugs are fixed by Google
class BraintreeSharedPreferences {

    private static final String SHARED_PREFS_ERROR_MESSAGE =
            "Unable to obtain a reference to encrypted shared preferences.";

    private static volatile BraintreeSharedPreferences INSTANCE;
    private static final String BRAINTREE_KEY_ALIAS = "com.braintreepayments.api.masterkey";
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

    private BraintreeSharedPreferences() {
    }

    static SharedPreferences getSharedPreferences(Context context) throws UnexpectedException {
        try {
            MasterKey masterKey = new MasterKey.Builder(context, BRAINTREE_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    BRAINTREE_SHARED_PREFS_FILENAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    String getString(Context context, String key, String fallback) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.getString(key, fallback);
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
        return fallback;
    }

    void putString(Context context, String key, String value) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .putString(key, value)
                        .apply();
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
    }

    boolean getBoolean(Context context, String key) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.getBoolean(key, false);
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
        return false;
    }

    void putBoolean(Context context, String key, boolean value) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .putBoolean(key, value)
                        .apply();
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
    }

    boolean containsKey(Context context, String key) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.contains(key);
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
        return false;
    }

    long getLong(Context context, String key) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.getLong(key, 0);
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
        return 0;
    }

    void putStringAndLong(Context context, String stringKey, String stringValue, String longKey, long longValue) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .putString(stringKey, stringValue)
                        .putLong(longKey, longValue)
                        .apply();
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
    }

    void clearSharedPreferences(Context context) throws UnexpectedException {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .clear()
                        .apply();
            } catch (SecurityException e) {
                throw new UnexpectedException(SHARED_PREFS_ERROR_MESSAGE, e);
            }
        }
    }
}
