package com.paypal.android.sdk.onetouch.core.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.paypal.android.sdk.onetouch.core.network.OtcEnvironment;

import java.util.UUID;

/**
 * This class contains various convenience methods on an application's mContext.
 */
public class ContextInspector {

    private static final String INSTALL_GUID = "InstallationGUID";

    private final Context mContext;
    private final SharedPreferences mPreferences;

    public ContextInspector(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mPreferences = mContext.getSharedPreferences(OtcEnvironment.getPrefsFile(), Context.MODE_PRIVATE);
    }

    /**
     * @return the installation GUID / the device's identifier. This has been a bunch of different things over time,
     * but now is an installation GUID. It is not stable across uninstalls but it's the best we can do.
     */
    public String getInstallationGUID() {
        String existingGUID = getStringPreference(INSTALL_GUID);
        if (existingGUID != null) {
            return existingGUID;
        } else {
            String newGuid = UUID.randomUUID().toString();
            setPreference(INSTALL_GUID, newGuid);

            return newGuid;
        }
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