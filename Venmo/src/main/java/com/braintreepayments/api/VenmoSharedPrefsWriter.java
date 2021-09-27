package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.security.GeneralSecurityException;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    VenmoSharedPrefsWriter() {}

    @VisibleForTesting
    void persistVenmoVaultOption(Context context, BraintreeSharedPreferences braintreeSharedPreferences, boolean shouldVault) {
        try {
            braintreeSharedPreferences.getSharedPreferences(context).edit()
                    .putBoolean(VAULT_VENMO_KEY, shouldVault)
                    .apply();
        } catch (GeneralSecurityException | IOException ignored) {
        }
    }

    void persistVenmoVaultOption(Context context, boolean shouldVault) {
        persistVenmoVaultOption(context, new BraintreeSharedPreferences(), shouldVault);
    }

    @VisibleForTesting
    boolean getVenmoVaultOption(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
        try {
            return braintreeSharedPreferences.getSharedPreferences(context)
                    .getBoolean(VAULT_VENMO_KEY, false);
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
    }

    boolean getVenmoVaultOption(Context context) {
        return getVenmoVaultOption(context, new BraintreeSharedPreferences());
    }
}
