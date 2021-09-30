package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

class BraintreeSharedPreferences {

    SharedPreferences getSharedPreferences(Context context) throws GeneralSecurityException, IOException {
        return getSharedPreferences(context, "BraintreeApi");
    }

    SharedPreferences getSharedPreferences(Context context, String filename) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                filename,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    String getString(Context context, String key) throws GeneralSecurityException, IOException {
        return getSharedPreferences(context)
                .getString(key, "");
    }

    String getString(Context context, String filename, String key) throws GeneralSecurityException, IOException {
        return getSharedPreferences(context, filename)
                .getString(key, "");
    }

    void putString(Context context, String key, String value) throws GeneralSecurityException, IOException {
        getSharedPreferences(context).edit()
                .putString(key, value)
                .apply();
    }

    void putString(Context context, String filename, String key, String value) throws GeneralSecurityException, IOException {
        getSharedPreferences(context, filename).edit()
                .putString(key, value)
                .apply();
    }

    boolean getBoolean(Context context, String key) throws GeneralSecurityException, IOException {
        return getSharedPreferences(context)
                .getBoolean(key, false);
    }

    void putBoolean(Context context, String key, boolean value) throws GeneralSecurityException, IOException {
        getSharedPreferences(context).edit()
                .putBoolean(key, value)
                .apply();
    }

    boolean containsKey(Context context, String key) throws GeneralSecurityException, IOException {
        return getSharedPreferences(context).contains(key);
    }

    long getLong(Context context, String key) throws GeneralSecurityException, IOException {
        return getSharedPreferences(context)
                .getLong(key, 0);
    }

    void putStringAndLong(Context context, String stringKey, String stringValue, String longKey, long longValue) throws GeneralSecurityException, IOException {
        getSharedPreferences(context).edit()
                .putString(stringKey, stringValue)
                .putLong(longKey, longValue)
                .apply();
    }

    void clearSharedPreferences(Context context) throws GeneralSecurityException, IOException {
        getSharedPreferences(context)
                .edit()
                .clear()
                .apply();
    }
}
