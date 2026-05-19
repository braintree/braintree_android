package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import com.braintreepayments.api.testutils.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class ConfigurationCacheTest {

    private lateinit var context: Context
    private lateinit var sut: ConfigurationCache
    private lateinit var sharedPreferences: BraintreeSharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sharedPreferences = BraintreeSharedPreferences.getInstance(context)
        sharedPreferences.clearSharedPreferences()
        sut = ConfigurationCache(sharedPreferences)
    }

    @Test(timeout = 1000)
    fun saveAndGetConfiguration_roundTrips() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val cacheKey = "test_cache_key_round_trip"
        val timestamp = 1000L

        sut.saveConfiguration(configuration, cacheKey, timestamp)
        val result = sut.getConfiguration(cacheKey, timestamp + 1)

        requireNotNull(result)
        val resultConfig = Configuration.fromJson(result)
        assertEquals("test", resultConfig.environment)
        assertEquals("integration_merchant_id", resultConfig.merchantId)
    }

    @Test(timeout = 1000)
    fun getConfiguration_returnsNullWhenNotSaved() {
        val result = sut.getConfiguration("nonexistent_key", System.currentTimeMillis())
        assertNull(result)
    }

    @Test(timeout = 1000)
    fun getConfiguration_returnsNullAfterTTLExpires() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val cacheKey = "test_cache_key_ttl_expired"
        val saveTimestamp = 0L
        val expiredTimestamp = TimeUnit.MINUTES.toMillis(5) + 1

        sut.saveConfiguration(configuration, cacheKey, saveTimestamp)
        val result = sut.getConfiguration(cacheKey, expiredTimestamp)

        assertNull(result)
    }

    @Test(timeout = 1000)
    fun getConfiguration_returnsValueJustBeforeTTLExpires() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val cacheKey = "test_cache_key_ttl_valid"
        val saveTimestamp = 0L
        val justBeforeExpiry = TimeUnit.MINUTES.toMillis(5) - 1

        sut.saveConfiguration(configuration, cacheKey, saveTimestamp)
        val result = sut.getConfiguration(cacheKey, justBeforeExpiry)

        assertNotNull(result)
    }

    @Test(timeout = 1000)
    fun saveConfiguration_overwritesPreviousValue() {
        val configA = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configB = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        val cacheKey = "test_cache_key_overwrite"
        val timestamp = 1000L

        sut.saveConfiguration(configA, cacheKey, timestamp)
        sut.saveConfiguration(configB, cacheKey, timestamp)

        val result = sut.getConfiguration(cacheKey, timestamp + 1)
        requireNotNull(result)
        val resultConfig = Configuration.fromJson(result)
        assertEquals("merchant-id", resultConfig.merchantId)
    }

    @Test(timeout = 1000)
    fun getConfiguration_withDifferentCacheKeys_returnsCorrectValues() {
        val configA = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configB = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        val keyA = "test_cache_key_a"
        val keyB = "test_cache_key_b"
        val timestamp = 1000L

        sut.saveConfiguration(configA, keyA, timestamp)
        sut.saveConfiguration(configB, keyB, timestamp)

        val resultA = sut.getConfiguration(keyA, timestamp + 1)
        val resultB = sut.getConfiguration(keyB, timestamp + 1)

        requireNotNull(resultA)
        requireNotNull(resultB)

        val configResultA = Configuration.fromJson(resultA)
        val configResultB = Configuration.fromJson(resultB)

        assertEquals("test", configResultA.environment)
        assertTrue(configResultB.isGooglePayEnabled)
    }
}
