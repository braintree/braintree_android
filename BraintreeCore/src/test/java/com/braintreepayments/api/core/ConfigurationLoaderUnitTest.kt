package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpMethod
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
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
import org.junit.Assert.assertSame
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
    private lateinit var threadScheduler: MockThreadScheduler

    @Before
    fun beforeEach() {
        configurationCache = mockk(relaxed = true)
        braintreeHttpClient = mockk(relaxed = true)
        callback = mockk(relaxed = true)
        authorization = mockk(relaxed = true)
        threadScheduler = MockThreadScheduler()
    }

    @Test
    fun loadConfiguration_loadsConfigurationOnBackgroundThread() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { configurationCache.getConfiguration(any(), any()) } returns null

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache, threadScheduler)
        sut.loadConfiguration(authorization, callback)

        // no http calls should be made on main thread
        verify(exactly = 0) { braintreeHttpClient.sendRequestSync(any(), any(), any()) }
        threadScheduler.flushBackgroundThread()

        val requestSlot = slot<InternalHttpRequest>()
        verify {
            braintreeHttpClient.sendRequestSync(capture(requestSlot), null, authorization)
        }

        val httpRequest = requestSlot.captured
        assertEquals(HttpMethod.GET, httpRequest.method)
        assertEquals("https://example.com/config?configVersion=3", httpRequest.path)
    }

    @Test
    fun loadConfiguration_onSuccess_callsBackConfigurationOnMainThread() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { configurationCache.getConfiguration(any(), any()) } returns null

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache, threadScheduler)
        sut.loadConfiguration(authorization, callback)

        val httpResponse =
            HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 0))
        every {
            braintreeHttpClient.sendRequestSync(any(), null, authorization)
        } returns httpResponse

        threadScheduler.flushBackgroundThread()
        // call back should happen on main thread
        verify(exactly = 0) { callback.onResult(any()) }

        threadScheduler.flushMainThread()
        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        val response = responseSlot.captured
        assertNotNull(response.configuration)
        assertNull(response.error)
        assertEquals(HttpResponseTiming(0, 0), response.timing)
    }

    @Test
    fun loadConfiguration_savesFetchedConfigurationToCache() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { configurationCache.getConfiguration(any(), any()) } returns null

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache, threadScheduler)
        sut.loadConfiguration(authorization, callback)

        val httpResponse =
            HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 0))
        every {
            braintreeHttpClient.sendRequestSync(any(), null, authorization)
        } returns httpResponse
        threadScheduler.flushBackgroundThread()

        val expectedConfigUrl = "https://example.com/config?configVersion=3"
        verify {
            configurationCache.putConfiguration(any(), authorization, expectedConfigUrl)
        }
    }

    @Test
    fun loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { configurationCache.getConfiguration(any(), any()) } returns null

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache, threadScheduler)
        sut.loadConfiguration(authorization, callback)

        val httpResponse = HttpResponse("not json", HttpResponseTiming(0, 0))
        every {
            braintreeHttpClient.sendRequestSync(any(), null, authorization)
        } returns httpResponse

        threadScheduler.flushBackgroundThread()
        // call back should happen on main thread
        verify(exactly = 0) { callback.onResult(any()) }

        threadScheduler.flushMainThread()
        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        val response = responseSlot.captured
        assertNull(response.configuration)
        assertTrue(response.error?.cause is JSONException)
        assertNull(response.timing)
    }

    @Test
    fun loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { configurationCache.getConfiguration(any(), any()) } returns null

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache, threadScheduler)
        sut.loadConfiguration(authorization, callback)

        val httpError = Exception("http error")
        every {
            braintreeHttpClient.sendRequestSync(any(), null, authorization)
        } throws httpError

        threadScheduler.flushBackgroundThread()
        // call back should happen on main thread
        verify(exactly = 0) { callback.onResult(any()) }

        threadScheduler.flushMainThread()
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
        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache, threadScheduler)
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

        val cachedConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        every {
            configurationCache.getConfiguration(authorization, "https://example.com/config?configVersion=3")
        } returns cachedConfiguration

        val sut = ConfigurationLoader(braintreeHttpClient, configurationCache, threadScheduler)
        sut.loadConfiguration(authorization, callback)

        threadScheduler.flushBackgroundThread()
        // call back should happen on main thread
        verify(exactly = 0) { callback.onResult(any()) }

        threadScheduler.flushMainThread()
        val responseSlot = slot<ConfigurationLoaderResponse>()
        verify { callback.onResult(capture(responseSlot)) }

        verify(exactly = 0) { braintreeHttpClient.sendRequestSync(any(), null, authorization) }
        val response = responseSlot.captured
        assertSame(cachedConfiguration, response.configuration)
        assertNull(response.error)
        assertNull(response.timing)
    }
}
