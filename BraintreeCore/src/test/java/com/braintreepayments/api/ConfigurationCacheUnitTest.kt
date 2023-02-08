package com.braintreepayments.api

import android.content.Context
import com.braintreepayments.api.Configuration.Companion.fromJson
import org.robolectric.RobolectricTestRunner
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ConfigurationCacheUnitTest {

    private var braintreeSharedPreferences: BraintreeSharedPreferences = mockk(relaxed = true)

    @Before
    fun beforeEach() {
        mockkStatic(BraintreeSharedPreferences::class)
        every { BraintreeSharedPreferences.getInstance(any()) } returns braintreeSharedPreferences
    }

    @Test
    fun saveConfiguration_savesConfigurationInSharedPrefs() {
        val configuration = fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val sut = ConfigurationCache(braintreeSharedPreferences!!)
        sut.saveConfiguration(configuration, "cacheKey", 123L)
        verify {
            braintreeSharedPreferences.putStringAndLong(
                "cacheKey",
                configuration.toJson(),
                "cacheKey_timestamp",
                123L
            )
        }
    }

    @Test(expected = BraintreeSharedPreferencesException::class)
    fun saveConfiguration_whenSharedPreferencesFails_forwardsException() {
        val configuration = fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val sut = ConfigurationCache(braintreeSharedPreferences)
        val sharedPrefsException = BraintreeSharedPreferencesException("unexpected exception")
        every {
            braintreeSharedPreferences.putStringAndLong(any(), any(), any(), any())
        } throws sharedPrefsException
        sut.saveConfiguration(configuration, "cacheKey", 123)
    }

    @Test
    fun getConfiguration_returnsConfigurationFromSharedPrefs() {
        val configuration = fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        every { braintreeSharedPreferences.containsKey("cacheKey_timestamp") } returns true
        every { braintreeSharedPreferences.getLong("cacheKey_timestamp") } returns 0L
        every { braintreeSharedPreferences.getString("cacheKey", "") } returns configuration.toJson()

        val sut = ConfigurationCache(braintreeSharedPreferences)
        sut.saveConfiguration(configuration, "cacheKey", 0)

        assertEquals(
            configuration.toJson(),
            sut.getConfiguration("cacheKey", TimeUnit.MINUTES.toMillis(5) - 1)
        )
    }

    @Test
    fun getConfiguration_whenCacheEntryExpires_returnsNull() {
        val configuration = fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        every { braintreeSharedPreferences.containsKey("cacheKey_timestamp") } returns true
        every { braintreeSharedPreferences.getLong("cacheKey_timestamp") } returns TimeUnit.MINUTES.toMillis(5)
        every { braintreeSharedPreferences.getString("cacheKey", "") } returns configuration.toJson()

        val sut = ConfigurationCache(braintreeSharedPreferences)
        sut.saveConfiguration(configuration, "cacheKey", 0)

        assertNull(sut.getConfiguration("cacheKey", TimeUnit.MINUTES.toMillis(20)))
    }

    @Test(expected = BraintreeSharedPreferencesException::class)
    fun getConfiguration_whenSharedPreferencesFails_forwardsException() {
        val sharedPrefsException = BraintreeSharedPreferencesException("unexpected exception")
        every { braintreeSharedPreferences.containsKey(any()) } throws sharedPrefsException
        every { braintreeSharedPreferences.getLong(any()) } throws sharedPrefsException
        every { braintreeSharedPreferences.getString(any(), any()) } throws sharedPrefsException

        val sut = ConfigurationCache(braintreeSharedPreferences)
        assertNull(sut.getConfiguration("cacheKey", TimeUnit.MINUTES.toMillis(5) - 1))
    }
}