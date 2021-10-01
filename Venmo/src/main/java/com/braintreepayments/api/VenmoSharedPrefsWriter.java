package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.security.GeneralSecurityException;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";
    private final BraintreeSharedPreferences braintreeSharedPreferences;

    VenmoSharedPrefsWriter() {
        this(new BraintreeSharedPreferences());
    }

    @VisibleForTesting
    VenmoSharedPrefsWriter(BraintreeSharedPreferences braintreeSharedPreferences) {
        this.braintreeSharedPreferences = braintreeSharedPreferences;
    }

    void persistVenmoVaultOption(Context context, boolean shouldVault) {
        try {
            braintreeSharedPreferences.putBoolean(context, VAULT_VENMO_KEY, shouldVault);
        } catch (GeneralSecurityException | IOException ignored) {
        }
    }

    boolean getVenmoVaultOption(Context context) {
        try {
            return braintreeSharedPreferences.getBoolean(context, VAULT_VENMO_KEY);
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
    }
}
