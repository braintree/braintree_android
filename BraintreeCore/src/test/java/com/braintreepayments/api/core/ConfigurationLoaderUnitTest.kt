package com.braintreepayments.api.core

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigurationLoaderUnitTest {

    private lateinit var configurationCache: ConfigurationCache
    private lateinit var braintreeHttpClient: BraintreeHttpClient
    private lateinit var callback: ConfigurationLoaderCallback
    private lateinit var authorization: Authorization

    @Before
    fun beforeEach() {
        configurationCache = mockk(relaxed = true)
        braintreeHttpClient = mockk(relaxed = true)
        callback = mockk(relaxed = true)
        authorization = mockk(relaxed = true)
    }

    @Test
    fun loadConfiguration_loadsConfigurationForTheCurrentEnvironment() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { configurationCache.getConfiguration(any(), any(), any()) } returns null

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
        every { configurationCache.getConfiguration(any(), any(), any()) } returns null
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

        verify {
            configurationCache.putConfiguration(
                any<Configuration>(),
                authorization,
                "https://example.com/config?configVersion=3",
                any()
            )
        }
    }

    @Test
    fun loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() {
        every { configurationCache.getConfiguration(any(), any(), any()) } returns null
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
        every { configurationCache.getConfiguration(any(), any(), any()) } returns null
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
        every { authorization.configUrl } returns "https://example.com/config"
        every { authorization.bearer } returns "bearer"
        every {
            configurationCache.getConfiguration(authorization, "https://example.com/config", any())
        } returns Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)

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
