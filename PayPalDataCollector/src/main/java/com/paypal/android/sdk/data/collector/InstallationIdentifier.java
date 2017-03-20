package com.paypal.android.sdk.data.collector;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class InstallationIdentifier {

    private static final String INSTALL_GUID = "InstallationGUID";

    public static String getInstallationGUID(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("PayPalOTC", Context.MODE_PRIVATE);
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
}
