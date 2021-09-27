package com.braintreepayments.api;

import android.content.Context;

import java.io.IOException;
import java.security.GeneralSecurityException;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    VenmoSharedPrefsWriter() {}

    void persistVenmoVaultOption(Context context, boolean shouldVault) {
        try {
            BraintreeSharedPreferences.getSharedPreferences(context).edit()
                    .putBoolean(VAULT_VENMO_KEY, shouldVault)
                    .apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    boolean getVenmoVaultOption(Context context) {
        try {
            return BraintreeSharedPreferences.getSharedPreferences(context)
                    .getBoolean(VAULT_VENMO_KEY, false);
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
    }
}
