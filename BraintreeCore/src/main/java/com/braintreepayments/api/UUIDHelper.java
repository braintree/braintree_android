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
        return getPersistentUUID(context, new BraintreeSharedPreferences());
    }

    @VisibleForTesting
    String getPersistentUUID(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
        String uuid = braintreeSharedPreferences.getString(context, BRAINTREE_UUID_KEY);

        if (uuid == null) {
            uuid = getFormattedUUID();
            braintreeSharedPreferences.putString(context, BRAINTREE_UUID_KEY, uuid);
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
        String existingGUID = braintreeSharedPreferences.getString(context, INSTALL_GUID);
        if (existingGUID != null) {
            return existingGUID;
        } else {
            String newGuid = UUID.randomUUID().toString();
            braintreeSharedPreferences.putString(context, INSTALL_GUID, newGuid);
            return newGuid;
        }
    }
}
