package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import com.braintreepayments.api.testutils.Fixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ConfigurationCacheUnitTest {

    private lateinit var braintreeSharedPreferences: BraintreeSharedPreferences

    @Before
    fun beforeEach() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        braintreeSharedPreferences =
            BraintreeSharedPreferences(context, "ConfigurationCacheUnitTest")
    }

    @After
    fun afterEach() {
        braintreeSharedPreferences.clearSharedPreferences()
    }

    @Test
    fun getConfiguration_returnsReturnsNullByDefault() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configUrl = "https://sample.com/url"

        val sut = ConfigurationCache(braintreeSharedPreferences)
        assertNull(
            sut.getConfiguration(authorization, configUrl, TimeUnit.MINUTES.toMillis(20))
        )
    }

    @Test
    fun getConfiguration_returnsConfigurationFromSharedPrefs() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val configUrl = "https://sample.com/url"

        val sut = ConfigurationCache(braintreeSharedPreferences)
        sut.putConfiguration(configuration, authorization, configUrl, 0L)

        assertEquals(
            configuration.toJson(),
            sut.getConfiguration(authorization, configUrl, TimeUnit.MINUTES.toMillis(5) - 1)
                ?.toJson()
        )
    }

    @Test
    fun getConfiguration_whenCacheEntryExpires_returnsNull() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val configUrl = "https://sample.com/url"

        val sut = ConfigurationCache(braintreeSharedPreferences)
        sut.putConfiguration(configuration, authorization, configUrl, 0L)

        assertNull(
            sut.getConfiguration(authorization, configUrl, TimeUnit.MINUTES.toMillis(20))
        )
    }
}
