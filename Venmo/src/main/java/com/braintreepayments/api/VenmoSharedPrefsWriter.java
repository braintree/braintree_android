package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    private final BraintreeSharedPreferences braintreeSharedPreferences;

    VenmoSharedPrefsWriter() {
        this(BraintreeSharedPreferences.getInstance());
    }

    @VisibleForTesting
    VenmoSharedPrefsWriter(BraintreeSharedPreferences braintreeSharedPreferences) {
        this.braintreeSharedPreferences = braintreeSharedPreferences;
    }

    void persistVenmoVaultOption(Context context, boolean shouldVault) throws UnexpectedException {
        braintreeSharedPreferences.putBoolean(context, VAULT_VENMO_KEY, shouldVault);
    }

    boolean getVenmoVaultOption(Context context) throws UnexpectedException {
        return braintreeSharedPreferences.getBoolean(context, VAULT_VENMO_KEY);
    }
}
