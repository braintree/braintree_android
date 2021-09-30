package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.security.GeneralSecurityException;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    VenmoSharedPrefsWriter() {}

    void persistVenmoVaultOption(Context context, boolean shouldVault) {
        persistVenmoVaultOption(context, new BraintreeSharedPreferences(), shouldVault);
    }

    @VisibleForTesting
    void persistVenmoVaultOption(Context context, BraintreeSharedPreferences braintreeSharedPreferences, boolean shouldVault) {
        try {
            braintreeSharedPreferences.putBoolean(context, VAULT_VENMO_KEY, shouldVault);
        } catch (GeneralSecurityException | IOException ignored) {
        }
    }

    boolean getVenmoVaultOption(Context context) {
        return getVenmoVaultOption(context, new BraintreeSharedPreferences());
    }

    @VisibleForTesting
    boolean getVenmoVaultOption(Context context, BraintreeSharedPreferences braintreeSharedPreferences) {
        try {
            return braintreeSharedPreferences.getBoolean(context, VAULT_VENMO_KEY);
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
    }
}
