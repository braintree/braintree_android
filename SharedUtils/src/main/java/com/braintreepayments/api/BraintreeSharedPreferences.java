package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

// TODO: consider removing Security exception ignore statements once crypto library
// bugs are fixed by Google
class BraintreeSharedPreferences {

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

    static SharedPreferences getSharedPreferences(Context context) {
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
        } catch (Exception ignored) {
            // In the rare case that we are unable to obtain a reference to encrypted shared
            // preferences, all preference update and retrieval logic will no-op
            return null;
        }
    }

    String getString(Context context, String key, String fallback) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.getString(key, fallback);
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
                return fallback;
            }
        }
        return fallback;
    }

    void putString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .putString(key, value)
                        .apply();
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
            }
        }
    }

    boolean getBoolean(Context context, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.getBoolean(key, false);
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
            }
        }
        return false;
    }

    void putBoolean(Context context, String key, boolean value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .putBoolean(key, value)
                        .apply();
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
            }
        }
    }

    boolean containsKey(Context context, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.contains(key);
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
                return false;
            }
        }
        return false;
    }

    long getLong(Context context, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                return sharedPreferences.getLong(key, 0);
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
                return 0;
            }
        }
        return 0;
    }

    void putStringAndLong(Context context, String stringKey, String stringValue, String longKey, long longValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .putString(stringKey, stringValue)
                        .putLong(longKey, longValue)
                        .apply();
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
            }
        }
    }

    void clearSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            try {
                sharedPreferences
                        .edit()
                        .clear()
                        .apply();
            } catch (SecurityException ignored) {
                // defensively guard against issues with shared preferences library
            }
        }
    }
}
