package com.paypal.android.sdk.onetouch.core.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * This class contains various convenience methods on an application's mContext.
 */
public class ContextInspector {
    private static final String TAG = ContextInspector.class.getSimpleName();

    private static final String INSTALL_GUID = "InstallationGUID";

    private final Context mContext;
    private final String mPrefsFileName;
    private final Crypto mCrypto;


    public ContextInspector(Context context, String prefsFileName, CryptoFactory cryptoFactory) {
        if (null == context) {
            throw new NullPointerException("context == null");
        }
        if (null == prefsFileName) {
            throw new NullPointerException("prefs == null");
        }

        this.mContext = context;
        this.mPrefsFileName = prefsFileName;
        this.mCrypto = cryptoFactory.createCrypto(this);
    }

    /**
     * Creates a new ContextInspector who's Crypto is just a passthrough (does not encrypt)
     *
     * @param context
     * @param prefsFileName
     */
    public ContextInspector(Context context, String prefsFileName) {
        this(context, prefsFileName, new CryptoFactory() {
            @Override
            public Crypto createCrypto(ContextInspector contextInspector) {
                return new Crypto() {
                    @Override
                    public String encryptIt(String value) {
                        return value;
                    }

                    @Override
                    public String decryptIt(String value) {
                        return value;
                    }
                };
            }
        });
    }

    /**
     * Returns true if any network is connected or connecting.
     *
     * @return
     */
    public boolean isNetworkAvailable() {
        int networkCount = 0;
        ConnectivityManager mgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null == mgr) {
            Log.w(Constants.PUBLIC_TAG, "Unable to retrieve Context.CONNECTIVITY_SERVICE. Ignoring.");
            return true;
        }
        if (null == mgr.getAllNetworkInfo()) {
            Log.w(Constants.PUBLIC_TAG, "ConnectivityManager.getAllNetworkInfo() returned null. Ignoring.");
            return true;
        }

        // Iterate over all of the available networks
        for (NetworkInfo info : mgr.getAllNetworkInfo()) {
            if (info.isConnectedOrConnecting()) {
                networkCount += 1;
            }
        }

        if (networkCount == 0) {
            Log.d(TAG, "no network available");
        }

        return networkCount > 0;
    }

    public int getPhoneType() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        return telephonyManager.getPhoneType();
    }

    public String getApplicationInfoName() {
        try {
            PackageManager packageManager = mContext.getPackageManager();
            PackageInfo i = packageManager.getPackageInfo(mContext.getPackageName(), 0);
            String name = i.applicationInfo.loadLabel(packageManager).toString();
            return name;
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public String getSimOperatorName() {
        try {
            TelephonyManager m = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            return m.getSimOperatorName();
        } catch (SecurityException e) {
            Log.d(TAG, e.toString());
            return null;
        }
    }

    /**
     * @return the installation GUID / the device's identifier. this has been a
     * bunch of different things over time but now is an installation GUID. it
     * is not stable across uninstalls but it's the best we can do.
     *
     * Skips the usual encryption/decryption, because this ID is stored in plaintext and used to seed the
     * encryptor/decryptor.
     */
    public String getInstallationGUID() {
        String existingGUID = mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).getString(
                INSTALL_GUID, null);
        if (existingGUID != null) {
            return existingGUID;
        } else {
            String newGuid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor =
                    mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).edit();
            editor.putString(INSTALL_GUID, newGuid);
            editor.commit();
            return newGuid;
        }
    }

    /**
     * retrieve a persisted string, or null if it does not exist and/or decryption failed
     *
     * @param key of the persisted value
     * @return the persisted value or default value if it does not exist
     */
    public String getStringPreference(String key) {
        return mCrypto.decryptIt(mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).getString(
                key, null));
    }


    public long getLongPreference(String key, long defaultValue) {
        return mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).getLong(
                key, defaultValue);
    }


    public boolean getBooleanPreference(String key, boolean defaultValue) {
        return mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).getBoolean(
                key, defaultValue);
    }

    /**
     * Persist a value, encrypting it first.
     *
     * @param key of the persisted value
     * @param value to persist
     */
    public void setPreference(String key, String value) {
        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).edit();
        editor.putString(key, mCrypto.encryptIt(value));
        editor.commit();
    }

    /**
     * Persist many values.  All strings are encrypted first.
     */
    public void setPreferences(Map<String, ?> mapToPersist) {
        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).edit();
        for (Entry<String, ?> entry : mapToPersist.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                editor.putString(key, mCrypto.encryptIt((String) value));
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else {
                // TODO add the others
                throw new RuntimeException(value.getClass() + " not supported");
            }
        }
        editor.commit();
    }

    public Context getContext() {
        return mContext;
    }

    public String encryptIt(String value) {
        return mCrypto.encryptIt(value);
    }

    public String decryptIt(String value) {
        return mCrypto.decryptIt(value);
    }
}