package com.braintreepayments.api;

import android.content.Context;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

class UUIDHelper {

    private static final String BRAINTREE_UUID_KEY = "braintreeUUID";

    /**
     * @param context Android Context
     * @param braintreeSharedPreferences {@link BraintreeSharedPreferences}
     * @return A persistent UUID for this application install.
     */
    static String getPersistentUUID(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
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

    static String getFormattedUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
