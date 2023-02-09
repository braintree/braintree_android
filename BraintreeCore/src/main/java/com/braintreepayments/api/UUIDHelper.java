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
        return getPersistentUUID(BraintreeSharedPreferences.getInstance(context));
    }

    @VisibleForTesting
    String getPersistentUUID(BraintreeSharedPreferences braintreeSharedPreferences) {
        String uuid = braintreeSharedPreferences.getString(BRAINTREE_UUID_KEY, null);

        if (uuid == null) {
            uuid = getFormattedUUID();
            braintreeSharedPreferences.putString(BRAINTREE_UUID_KEY, uuid);
        }
        return uuid;
    }

    String getFormattedUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    String getInstallationGUID(Context context) {
        return getInstallationGUID(BraintreeSharedPreferences.getInstance(context));
    }

    @VisibleForTesting
    String getInstallationGUID(BraintreeSharedPreferences braintreeSharedPreferences) {
        String installationGUID = braintreeSharedPreferences.getString(INSTALL_GUID, null);

        if (installationGUID == null) {
            installationGUID = UUID.randomUUID().toString();
            braintreeSharedPreferences.putString(INSTALL_GUID, installationGUID);
        }
        return installationGUID;
    }
}
