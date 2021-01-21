package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

class PayPalInstallationIdentifier {

    private static final String INSTALL_GUID = "InstallationGUID";
    private static final String SHARED_PREFS_NAMESPACE = "com.braintreepayments.api.paypal";

    String getInstallationGUID(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFS_NAMESPACE, Context.MODE_PRIVATE);
        if (preferences != null) {
            String existingGUID = preferences.getString(INSTALL_GUID, null);
            if (existingGUID != null) {
                return existingGUID;
            } else {
                String newGuid = UUID.randomUUID().toString();
                preferences.edit()
                        .putString(INSTALL_GUID, newGuid)
                        .apply();

                return newGuid;
            }
        }
        return null;
    }
}
