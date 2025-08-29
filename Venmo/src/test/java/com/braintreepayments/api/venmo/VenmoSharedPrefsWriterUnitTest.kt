package com.braintreepayments.api.venmo

import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class VenmoSharedPrefsWriterUnitTest {
    private val braintreeSharedPreferences: BraintreeSharedPreferences = mockk(relaxed = true)

    @Test
    fun `persistVenmoVaultOption persists vault option`() {
        val sut = VenmoSharedPrefsWriter()
        sut.persistVenmoVaultOption(braintreeSharedPreferences, true)
        verify { braintreeSharedPreferences.putBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true) }
    }

    @Test
    fun `getVenmoVaultOption retrieves vault option from SharedPrefs`() {
        val sut = VenmoSharedPrefsWriter()
        sut.getVenmoVaultOption(braintreeSharedPreferences)
        verify { braintreeSharedPreferences.getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY") }
    }
}
