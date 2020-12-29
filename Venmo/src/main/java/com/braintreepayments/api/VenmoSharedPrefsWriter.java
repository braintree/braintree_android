package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.internal.BraintreeSharedPreferences;

public class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    public VenmoSharedPrefsWriter() {}

    public void persistVenmoVaultOption(Context context, boolean shouldVault) {
        BraintreeSharedPreferences.getSharedPreferences(context).edit()
                .putBoolean(VAULT_VENMO_KEY, shouldVault)
                .apply();
    }

    public boolean getVenmoVaultOption(Context context) {
        return BraintreeSharedPreferences.getSharedPreferences(context)
                .getBoolean(VAULT_VENMO_KEY, false);
    }
}
