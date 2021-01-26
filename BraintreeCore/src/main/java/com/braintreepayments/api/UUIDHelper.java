package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class UUIDHelper {

    private static final String BRAINTREE_UUID_KEY = "braintreeUUID";

    /**
     * @param context
     * @return A persistent UUID for this application install.
     */
    public static String getPersistentUUID(Context context) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(context);

        String uuid = prefs.getString(BRAINTREE_UUID_KEY, null);
        if (uuid == null) {
            uuid = getFormattedUUID();
            prefs.edit().putString(BRAINTREE_UUID_KEY, uuid).apply();
        }

        return uuid;
    }

    public static String getFormattedUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
