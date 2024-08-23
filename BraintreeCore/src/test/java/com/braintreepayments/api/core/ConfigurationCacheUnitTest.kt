package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ConfigurationCacheUnitTest {

    private var braintreeSharedPreferences: BraintreeSharedPreferences = mockk(relaxed = true)

    @Test
    fun putConfiguration_putsConfigurationInSharedPrefs() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val configUrl = "https://sample.com/url"

        val sut = ConfigurationCache(braintreeSharedPreferences)
        sut.putConfiguration(configuration, authorization, configUrl, 123L)

        val expectedCacheKey = "https://sample.com/url${Fixtures.TOKENIZATION_KEY}"
        verify {
            braintreeSharedPreferences.putStringAndLong(
                expectedCacheKey,
                configuration.toJson(),
                "cacheKey_timestamp",
                123L
            )
        }
    }

    @Test
    fun getConfiguration_returnsConfigurationFromSharedPrefs() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val configUrl = "https://sample.com/url"

        val expectedCacheKey = "${configUrl}${Fixtures.TOKENIZATION_KEY}"
        every { braintreeSharedPreferences.containsKey("cacheKey_timestamp") } returns true
        every { braintreeSharedPreferences.getLong("cacheKey_timestamp") } returns 0L
        every {
            braintreeSharedPreferences.getString(expectedCacheKey, "")
        } returns configuration.toJson()

        val sut = ConfigurationCache(braintreeSharedPreferences)
        assertEquals(
            configuration.toJson(),
            sut.getConfiguration(authorization, configUrl, TimeUnit.MINUTES.toMillis(5) - 1)
        )
    }

    @Test
    fun getConfiguration_whenCacheEntryExpires_returnsNull() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val configUrl = "https://sample.com/url"

        val expectedCacheKey = "${configUrl}${Fixtures.TOKENIZATION_KEY}"
        every { braintreeSharedPreferences.containsKey("cacheKey_timestamp") } returns true
        every {
            braintreeSharedPreferences.getLong("cacheKey_timestamp")
        } returns TimeUnit.MINUTES.toMillis(5)

        every {
            braintreeSharedPreferences.getString(expectedCacheKey, "")
        } returns configuration.toJson()

        val sut = ConfigurationCache(braintreeSharedPreferences)
        assertNull(
            sut.getConfiguration(authorization, configUrl, TimeUnit.MINUTES.toMillis(20))
        )
    }
}
