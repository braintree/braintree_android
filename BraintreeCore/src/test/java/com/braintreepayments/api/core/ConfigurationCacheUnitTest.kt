package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import com.braintreepayments.api.sharedutils.Time
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.every
import io.mockk.mockk
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

    private lateinit var time: Time
    private lateinit var braintreeSharedPreferences: BraintreeSharedPreferences

    @Before
    fun beforeEach() {
        time = mockk()
        val context = ApplicationProvider.getApplicationContext<Context>()
        braintreeSharedPreferences = BraintreeSharedPreferences.getInstance(context)
    }

    @After
    fun afterEach() {
        braintreeSharedPreferences.clearSharedPreferences()
    }

    @Test
    fun getConfiguration_returnsReturnsNullByDefault() {
        val sut = ConfigurationCache(braintreeSharedPreferences, time)

        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configUrl = "https://sample.com/url"
        every { time.currentTime } returns TimeUnit.MINUTES.toMillis(20)

        assertNull(sut.getConfiguration(authorization, configUrl))
    }

    @Test
    fun getConfiguration_returnsConfigurationFromSharedPrefs() {
        val sut = ConfigurationCache(braintreeSharedPreferences, time)

        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val configUrl = "https://sample.com/url"

        every { time.currentTime } returns 0L
        sut.putConfiguration(configuration, authorization, configUrl)

        every { time.currentTime } returns (TimeUnit.MINUTES.toMillis(5) - 1)
        val expected = configuration.toJson()
        val actual = sut.getConfiguration(authorization, configUrl)?.toJson()
        assertEquals(expected, actual)
    }

    @Test
    fun getConfiguration_whenCacheEntryExpires_returnsNull() {
        val sut = ConfigurationCache(braintreeSharedPreferences, time)

        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        val configUrl = "https://sample.com/url"

        every { time.currentTime } returns 0L
        sut.putConfiguration(configuration, authorization, configUrl)

        every { time.currentTime } returns TimeUnit.MINUTES.toMillis(20)
        assertNull(sut.getConfiguration(authorization, configUrl))
    }
}
