package com.braintreepayments.api;

import android.content.Context;

class VenmoSharedPrefsWriter {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";
    private static final String PAYMENT_CONTEXT_ID_KEY = "com.braintreepayments.api.Venmo.PAYMENT_CONTEXT_ID_KEY";

    VenmoSharedPrefsWriter() {}

    void persistVenmoVaultOption(Context context, boolean shouldVault) {
        BraintreeSharedPreferences.getSharedPreferences(context).edit()
                .putBoolean(VAULT_VENMO_KEY, shouldVault)
                .apply();
    }

    void persistVenmoPaymentContextId(Context context, String paymentContextId) {
        if (paymentContextId != null) {
            BraintreeSharedPreferences.getSharedPreferences(context).edit()
                    .putString(PAYMENT_CONTEXT_ID_KEY, paymentContextId)
                    .apply();
        }
    }

    boolean getVenmoVaultOption(Context context) {
        return BraintreeSharedPreferences.getSharedPreferences(context)
                .getBoolean(VAULT_VENMO_KEY, false);
    }

    String getVenmoPaymentContextId(Context context) {
        return BraintreeSharedPreferences.getSharedPreferences(context)
                .getString(PAYMENT_CONTEXT_ID_KEY, null);
    }
}
