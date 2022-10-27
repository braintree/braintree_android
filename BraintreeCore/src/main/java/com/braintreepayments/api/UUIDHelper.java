package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.util.UUID;

class UUIDHelper {

    private static final String BRAINTREE_UUID_KEY = "braintreeUUID";
    private static final String INSTALL_GUID = "InstallationGUID";

    /**
     * @param context Android Context
     * @return A persistent UUID for this application install.
     */
    String getPersistentUUID(Context context) {
        return getPersistentUUID(context, BraintreeSharedPreferences.getInstance());
    }

    @VisibleForTesting
    String getPersistentUUID(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
        String uuid = null;
        try {
            uuid = braintreeSharedPreferences.getString(BRAINTREE_UUID_KEY, null);
        } catch (UnexpectedException ignored) {
            // protect against shared prefs failure: default to creating a new UUID in this scenario
        }

        if (uuid == null) {
            uuid = getFormattedUUID();
            try {
                braintreeSharedPreferences.putString(BRAINTREE_UUID_KEY, uuid);
            } catch (UnexpectedException ignored) {
                // protect against shared prefs failure: no-op when we're unable to persist the UUID
            }
        }

        return uuid;
    }

    String getFormattedUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    String getInstallationGUID(Context context) {
        return getInstallationGUID(context, BraintreeSharedPreferences.getInstance());
    }

    @VisibleForTesting
    String getInstallationGUID(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
        String installationGUID = null;
        try {
            installationGUID = braintreeSharedPreferences.getString(INSTALL_GUID, null);
        } catch (UnexpectedException ignored) {
            // protect against shared prefs failure: default to creating a new GUID in this scenario
        }

        if (installationGUID == null) {
            installationGUID = UUID.randomUUID().toString();
            try {
                braintreeSharedPreferences.putString(INSTALL_GUID, installationGUID);
            } catch (UnexpectedException ignored) {
                // protect against shared prefs failure: no-op when we're unable to persist the GUID
            }
        }
        return installationGUID;
    }
}
