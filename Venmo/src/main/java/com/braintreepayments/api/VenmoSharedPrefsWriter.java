package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    void persistVenmoVaultOption(Context context, boolean shouldVault) throws BraintreeSharedPreferencesException {
        persistVenmoVaultOption(BraintreeSharedPreferences.getInstance(context), shouldVault);
    }

    @VisibleForTesting
    void persistVenmoVaultOption(BraintreeSharedPreferences braintreeSharedPreferences, boolean shouldVault) throws BraintreeSharedPreferencesException {
        braintreeSharedPreferences.putBoolean(VAULT_VENMO_KEY, shouldVault);
    }

    boolean getVenmoVaultOption(Context context) throws BraintreeSharedPreferencesException {
        return getVenmoVaultOption(BraintreeSharedPreferences.getInstance(context));
    }

    @VisibleForTesting
    boolean getVenmoVaultOption(BraintreeSharedPreferences braintreeSharedPreferences) throws BraintreeSharedPreferencesException {
        return braintreeSharedPreferences.getBoolean(VAULT_VENMO_KEY);
    }
}
