package com.paypal.android.sdk.onetouch.core.base;

import android.content.Context;
import android.content.SharedPreferences;

import com.paypal.android.sdk.onetouch.core.network.OtcEnvironment;

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

    public ContextInspector(Context context, CryptoFactory cryptoFactory) {
        if (null == context) {
            throw new NullPointerException("context == null");
        }

        mContext = context;
        mPrefsFileName = OtcEnvironment.getPrefsFile();
        mCrypto = cryptoFactory.createCrypto(this);
    }

    /**
     * Creates a new ContextInspector who's Crypto is just a passthrough (does not encrypt)
     *
     * @param context
     */
    public ContextInspector(Context context) {
        this(context, new CryptoFactory() {
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
     * @return the installation GUID / the device's identifier. this has been a bunch of different
     * things over time but now is an installation GUID. it is not stable across uninstalls but it's
     * the best we can do.
     * <p>
     * Skips the usual encryption/decryption, because this ID is stored in plaintext and used to
     * seed the encryptor/decryptor.
     */
    public String getInstallationGUID() {
        String existingGUID =
                mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE).getString(
                        INSTALL_GUID, null);
        if (existingGUID != null) {
            return existingGUID;
        } else {
            String newGuid = UUID.randomUUID().toString();
            mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE)
                    .edit()
                    .putString(INSTALL_GUID, newGuid)
                    .apply();

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
        return mCrypto.decryptIt(mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE)
                        .getString(key, null));
    }

    public long getLongPreference(String key, long defaultValue) {
        return mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE)
                .getLong(key, defaultValue);
    }

    public boolean getBooleanPreference(String key, boolean defaultValue) {
        return mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE)
                .getBoolean(key, defaultValue);
    }

    /**
     * Persist a value, encrypting it first.
     *
     * @param key of the persisted value
     * @param value to persist
     */
    public void setPreference(String key, String value) {
        mContext.getSharedPreferences(mPrefsFileName, Context.MODE_PRIVATE)
                .edit()
                .putString(key, mCrypto.encryptIt(value))
                .apply();
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
        editor.apply();
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