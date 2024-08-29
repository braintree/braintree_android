package com.braintreepayments.api.venmo

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences

internal class VenmoSharedPrefsWriter {
    fun persistVenmoVaultOption(context: Context?, shouldVault: Boolean) {
        persistVenmoVaultOption(BraintreeSharedPreferences.getInstance(context), shouldVault)
    }

    @VisibleForTesting
    fun persistVenmoVaultOption(
        braintreeSharedPreferences: BraintreeSharedPreferences,
        shouldVault: Boolean
    ) {
        braintreeSharedPreferences.putBoolean(VAULT_VENMO_KEY, shouldVault)
    }

    fun getVenmoVaultOption(context: Context?): Boolean {
        return getVenmoVaultOption(BraintreeSharedPreferences.getInstance(context))
    }

    @VisibleForTesting
    fun getVenmoVaultOption(braintreeSharedPreferences: BraintreeSharedPreferences): Boolean {
        return braintreeSharedPreferences.getBoolean(VAULT_VENMO_KEY)
    }

    companion object {
        private const val VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY"
    }
}
