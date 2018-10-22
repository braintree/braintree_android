package com.paypal.android.sdk.onetouch.core.base;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

public class ContextInspector {

    private final Context mContext;
    private final SharedPreferences mPreferences;

    public ContextInspector(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mPreferences = mContext.getSharedPreferences("PayPalOTC", Context.MODE_PRIVATE);
    }

    public String getStringPreference(String key) {
        return mPreferences.getString(key, null);
    }

    public long getLongPreference(String key, long defaultValue) {
        return mPreferences.getLong(key, defaultValue);
    }

    public boolean getBooleanPreference(String key, boolean defaultValue) {
        return mPreferences.getBoolean(key, defaultValue);
    }

    public void setPreference(String key, String value) {
        mPreferences.edit()
                .putString(key, value)
                .apply();
    }

    public void setPreference(String key, long value) {
        mPreferences.edit()
                .putLong(key, value)
                .apply();
    }

    public void setPreference(String key, boolean value) {
        mPreferences.edit()
                .putBoolean(key, value)
                .apply();
    }

    public Context getContext() {
        return mContext;
    }
}