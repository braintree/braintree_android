package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

class PayPalInstallationIdentifier {

    private static final String INSTALL_GUID = "InstallationGUID";
    private static final String SHARED_PREFS_NAMESPACE = "com.braintreepayments.api.paypal";

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
