package com.braintreepayments.api

import com.braintreepayments.api.Configuration.Companion.fromJson
import org.robolectric.RobolectricTestRunner
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ConfigurationCacheUnitTest {

    private var braintreeSharedPreferences: BraintreeSharedPreferences = mockk(relaxed = true)

    @Test
    fun saveConfiguration_savesConfigurationInSharedPrefs() {
        val configuration = fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val sut = ConfigurationCache(braintreeSharedPreferences)
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
}
