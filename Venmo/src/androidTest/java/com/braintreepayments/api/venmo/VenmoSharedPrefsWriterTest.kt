package com.braintreepayments.api.venmo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoSharedPrefsWriterTest {

    private lateinit var context: Context
    private lateinit var sut: VenmoSharedPrefsWriter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sut = VenmoSharedPrefsWriter()
    }

    @Test
    fun getVenmoVaultOption_defaultsToFalse() {
        assertFalse(sut.getVenmoVaultOption(context))
    }

    @Test
    fun persistVenmoVaultOption_true_getVenmoVaultOption_returnsTrue() {
        sut.persistVenmoVaultOption(context, true)

        assertTrue(sut.getVenmoVaultOption(context))
    }

    @Test
    fun persistVenmoVaultOption_false_getVenmoVaultOption_returnsFalse() {
        sut.persistVenmoVaultOption(context, true)
        sut.persistVenmoVaultOption(context, false)

        assertFalse(sut.getVenmoVaultOption(context))
    }
}
