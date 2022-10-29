package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
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

    private final SharedPreferences sharedPreferences;
    private final BraintreeSharedPreferencesException createException;

    static BraintreeSharedPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BraintreeSharedPreferences.class) {
                // double check that instance was not created in another thread
                if (INSTANCE == null) {
                    try {
                        INSTANCE = new BraintreeSharedPreferences(createSharedPreferencesInstance(context));
                    } catch (BraintreeSharedPreferencesException e) {
                        INSTANCE = new BraintreeSharedPreferences(e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    private static SharedPreferences createSharedPreferencesInstance(Context context) throws BraintreeSharedPreferencesException {
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
            throw new BraintreeSharedPreferencesException(SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    BraintreeSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.createException = null;
    }

    BraintreeSharedPreferences(BraintreeSharedPreferencesException createException) {
        this.sharedPreferences = null;
        this.createException = createException;
    }

    @NonNull
    private SharedPreferences getSharedPreferences() throws BraintreeSharedPreferencesException {
        if (createException != null) {
            throw createException;
        } else if (sharedPreferences == null) {
            throw new BraintreeSharedPreferencesException(SHARED_PREFS_ERROR_MESSAGE);
        }
        return sharedPreferences;
    }

    String getString(String key, String fallback) throws BraintreeSharedPreferencesException {
        try {
            return getSharedPreferences().getString(key, fallback);
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException(SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    void putString(String key, String value) throws BraintreeSharedPreferencesException {
        try {
            getSharedPreferences()
                    .edit()
                    .putString(key, value)
                    .apply();
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException(SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    boolean getBoolean(String key) throws BraintreeSharedPreferencesException {
        try {
            return getSharedPreferences().getBoolean(key, false);
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException(SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    void putBoolean(String key, boolean value) throws BraintreeSharedPreferencesException {
        try {
            getSharedPreferences()
                    .edit()
                    .putBoolean(key, value)
                    .apply();
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException(SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    boolean containsKey(String key) throws BraintreeSharedPreferencesException {
        try {
            return getSharedPreferences().contains(key);
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException(SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    long getLong(String key) throws BraintreeSharedPreferencesException {
        try {
            return getSharedPreferences().getLong(key, 0);
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException (SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    void putStringAndLong(String stringKey, String stringValue, String longKey, long longValue) throws BraintreeSharedPreferencesException {
        try {
            getSharedPreferences()
                    .edit()
                    .putString(stringKey, stringValue)
                    .putLong(longKey, longValue)
                    .apply();
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException (SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }

    void clearSharedPreferences() throws BraintreeSharedPreferencesException {
        try {
            getSharedPreferences()
                    .edit()
                    .clear()
                    .apply();
        } catch (SecurityException e) {
            throw new BraintreeSharedPreferencesException (SHARED_PREFS_ERROR_MESSAGE, e);
        }
    }
}
