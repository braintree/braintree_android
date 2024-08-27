package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.Scheduler
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockThreadScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private lateinit var threadScheduler: Scheduler

    @Before
    fun beforeEach() {
        configurationCache = mockk(relaxed = true)
        braintreeHttpClient = mockk(relaxed = true)
        callback = mockk(relaxed = true)
        authorization = mockk(relaxed = true)
        threadScheduler = MockThreadScheduler()
    }

    @Test
    fun loadConfiguration_loadsConfigurationForTheCurrentEnvironment() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { configurationCache.getConfiguration(any(), any()) } returns null

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

        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        val response = responseSlot.captured
        assertNotNull(response.configuration)
        assertNull(response.error)
        assertEquals(HttpResponseTiming(0, 0), response.timing)
    }

    @Test
    fun loadConfiguration_savesFetchedConfigurationToCache() {
        every { configurationCache.getConfiguration(any(), any()) } returns null
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
                "https://example.com/config?configVersion=3"
            )
        }
    }

    @Test
    fun loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() {
        every { configurationCache.getConfiguration(any(), any()) } returns null
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

        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        val response = responseSlot.captured
        assertNull(response.configuration)
        assertTrue(response.error is JSONException)
        assertNull(response.timing)
    }

    @Test
    fun loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() {
        every { configurationCache.getConfiguration(any(), any()) } returns null
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

        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        val response = responseSlot.captured
        assertNull(response.configuration)
        assertNull(response.timing)

        val error = response.error
        assertTrue(error is ConfigurationException)
        assertEquals("Request for configuration has failed: http error", error?.message)
    }

    @Test
    fun loadConfiguration_whenInvalidToken_forwardsExceptionToCallback() {
        val authorization: Authorization = InvalidAuthorization("invalid", "token invalid")
        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)

        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        val response = responseSlot.captured
        assertNull(response.configuration)
        assertNull(response.timing)

        val error = response.error
        assertTrue(error is BraintreeException)
        assertEquals("token invalid", error?.message)
    }

    @Test
    fun loadConfiguration_whenCachedConfigurationAvailable_loadsConfigurationFromCache() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { authorization.bearer } returns "bearer"
        every {
            configurationCache.getConfiguration(authorization, "https://example.com/config")
        } returns Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache)
        sut.loadConfiguration(authorization, callback)

        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        verify(exactly = 0) { braintreeHttpClient.sendRequestSync(any(), null, authorization) }
        val response = responseSlot.captured
        assertNotNull(response.configuration)
        assertNull(response.error)
        assertNull(response.timing)
    }
}
