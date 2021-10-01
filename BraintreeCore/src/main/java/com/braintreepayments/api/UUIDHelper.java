package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

class UUIDHelper {

    private static final String BRAINTREE_UUID_KEY = "braintreeUUID";
    private static final String INSTALL_GUID = "InstallationGUID";
    private static final String SHARED_PREFS_NAMESPACE = "com.braintreepayments.api.paypal";

    /**
     * @param context Android Context
     * @return A persistent UUID for this application install.
     */
    String getPersistentUUID(Context context) {
        return getPersistentUUID(context, new BraintreeSharedPreferences());
    }

    @VisibleForTesting
    String getPersistentUUID(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
        String uuid = null;
        try {
            uuid = braintreeSharedPreferences.getString(context, BRAINTREE_UUID_KEY);
        } catch (GeneralSecurityException | IOException ignored) {}

        if (uuid == null) {
            uuid = getFormattedUUID();
            try {
                braintreeSharedPreferences.putString(context, BRAINTREE_UUID_KEY, uuid);
            } catch (GeneralSecurityException | IOException ignored) {}
        }

        return uuid;
    }

    String getFormattedUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    String getInstallationGUID(Context context) {
        return getInstallationGUID(context, new BraintreeSharedPreferences());
    }

    @VisibleForTesting
    String getInstallationGUID(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
        try {
            String existingGUID = braintreeSharedPreferences.getString(context, SHARED_PREFS_NAMESPACE, INSTALL_GUID);
            if (existingGUID != null) {
                return existingGUID;
            } else {
                String newGuid = UUID.randomUUID().toString();
                braintreeSharedPreferences.putString(context, SHARED_PREFS_NAMESPACE, INSTALL_GUID, newGuid);
                return newGuid;
            }
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }
    }
}
