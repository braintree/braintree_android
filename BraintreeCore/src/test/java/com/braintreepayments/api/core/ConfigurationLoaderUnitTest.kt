package com.braintreepayments.api.core

import android.util.Base64
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigurationLoaderUnitTest {
    private var configurationCache: ConfigurationCache = mockk(relaxed = true)
    private var braintreeHttpClient: BraintreeHttpClient = mockk(relaxed = true)
    private var callback: ConfigurationLoaderCallback = mockk(relaxed = true)
    private var authorization: Authorization = mockk(relaxed = true)

    @Test
    fun loadConfiguration_loadsConfigurationForTheCurrentEnvironment() {

        every { authorization.configUrl } returns "https://example.com/config"

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)

        val httpRequestSlot = slot<InternalHttpRequest>()
        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.sendRequest(
                capture(httpRequestSlot),
                null,
                authorization,
                capture(callbackSlot)
            )
        }

        val expectedConfigUrl = "https://example.com/config?configVersion=3"
        assertEquals(expectedConfigUrl, httpRequestSlot.captured.path)

        val httpResponseCallback = callbackSlot.captured
        httpResponseCallback.onResult(
            HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 0)), null
        )

        verify { callback.onResult(ofType(Configuration::class), null, HttpResponseTiming(0, 0)) }
    }

    @Test
    fun loadConfiguration_savesFetchedConfigurationToCache() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { authorization.bearer } returns "bearer"

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)

        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.sendRequest(
                any(),
                null,
                authorization,
                capture(callbackSlot)
            )
        }

        val httpResponseCallback = callbackSlot.captured
        httpResponseCallback.onResult(
            HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 0)), null
        )
        val cacheKey = Base64.encodeToString(
            "https://example.com/config?configVersion=3bearer".toByteArray(),
            0
        )

        verify {
            configurationCache.saveConfiguration(ofType(Configuration::class), cacheKey)
        }
    }

    @Test
    fun loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() {
        every { authorization.configUrl } returns "https://example.com/config"
        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)

        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.sendRequest(
                any(),
                null,
                authorization,
                capture(callbackSlot)
            )
        }

        val httpResponseCallback = callbackSlot.captured
        httpResponseCallback.onResult(HttpResponse("not json", HttpResponseTiming(0, 0)), null)
        verify {
            callback.onResult(null, ofType(JSONException::class), null)
        }
    }

    @Test
    fun loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() {
        every { authorization.configUrl } returns "https://example.com/config"
        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)

        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.sendRequest(
                any(),
                null,
                authorization,
                capture(callbackSlot)
            )
        }

        val httpResponseCallback = callbackSlot.captured
        val httpError = Exception("http error")
        httpResponseCallback.onResult(null, httpError)
        val errorSlot = slot<Exception>()
        verify {
            callback.onResult(null, capture(errorSlot), null)
        }

        val error = errorSlot.captured as ConfigurationException
        assertEquals(
            "Request for configuration has failed: http error",
            error.message
        )
    }

    @Test
    fun loadConfiguration_whenInvalidToken_forwardsExceptionToCallback() {
        val authorization: Authorization = InvalidAuthorization("invalid", "token invalid")
        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)
        val errorSlot = slot<BraintreeException>()
        verify {
            callback.onResult(null, capture(errorSlot), null)
        }

        val exception = errorSlot.captured
        assertEquals("token invalid", exception.message)
    }

    @Test
    fun loadConfiguration_whenCachedConfigurationAvailable_loadsConfigurationFromCache() {
        val cacheKey = Base64.encodeToString(
            "https://example.com/config?configVersion=3bearer".toByteArray(),
            0
        )
        every { authorization.configUrl } returns "https://example.com/config"
        every { authorization.bearer } returns "bearer"
        every { configurationCache.getConfiguration(cacheKey) } returns Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)

        verify(exactly = 0) {
            braintreeHttpClient.sendRequest(
                any(),
                null,
                authorization,
                any()
            )
        }
        verify { callback.onResult(ofType(Configuration::class), null, null) }
    }
}
