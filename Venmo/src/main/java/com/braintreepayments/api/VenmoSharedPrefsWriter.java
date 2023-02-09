package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    void persistVenmoVaultOption(Context context, boolean shouldVault) {
        persistVenmoVaultOption(BraintreeSharedPreferences.getInstance(context), shouldVault);
    }

    @VisibleForTesting
    void persistVenmoVaultOption(BraintreeSharedPreferences braintreeSharedPreferences, boolean shouldVault) {
        braintreeSharedPreferences.putBoolean(VAULT_VENMO_KEY, shouldVault);
    }

    boolean getVenmoVaultOption(Context context) {
        return getVenmoVaultOption(BraintreeSharedPreferences.getInstance(context));
    }

    @VisibleForTesting
    boolean getVenmoVaultOption(BraintreeSharedPreferences braintreeSharedPreferences) {
        return braintreeSharedPreferences.getBoolean(VAULT_VENMO_KEY);
    }
}
