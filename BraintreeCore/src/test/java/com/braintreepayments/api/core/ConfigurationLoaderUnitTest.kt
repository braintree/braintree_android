package com.braintreepayments.api.core

import android.util.Base64
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfigurationLoaderUnitTest {
    private val configurationCache: ConfigurationCache = mockk(relaxed = true)
    private val braintreeHttpClient: BraintreeHttpClient = mockk(relaxed = true)
    private val callback: ConfigurationLoaderCallback = mockk(relaxed = true)
    private val authorization: Authorization = mockk(relaxed = true)
    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)

    private lateinit var sut: ConfigurationLoader

    @Before
    fun setUp() {
        every { merchantRepository.authorization } returns authorization
    }

    @Test
    fun loadConfiguration_loadsConfigurationForTheCurrentEnvironment() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"
        every { merchantRepository.authorization } returns authorization

        val mockResponse = HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.get(
                "https://example.com/config?configVersion=3",
                null,
                authorization
            )
        } returns mockResponse

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createConfigurationLoader(testDispatcher, testScope)

        sut.loadConfiguration(callback)
        advanceUntilIdle()

        val successSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(successSlot)) }

        assertTrue(successSlot.captured is ConfigurationLoaderResult.Success)
    }

    @Test
    fun loadConfiguration_savesFetchedConfigurationToCache() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"
        every { authorization.bearer } returns "bearer"

        val mockResponse = HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.get(
                "https://example.com/config?configVersion=3",
                null,
                authorization
            )
        } returns mockResponse

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createConfigurationLoader(testDispatcher, testScope)

        sut.loadConfiguration(callback)
        advanceUntilIdle()

        val cacheKey = Base64.encodeToString(
            "https://example.com/config?configVersion=3bearer".toByteArray(),
            0
        )

        verify {
            configurationCache.saveConfiguration(ofType(Configuration::class), cacheKey)
        }
    }

    @Test
    fun loadConfiguration_onJSONParsingError_forwardsExceptionToErrorResponseListener() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"

        val mockResponse = HttpResponse("not json", HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.get(
                ofType(String::class),
                null,
                authorization
            )
        } returns mockResponse

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createConfigurationLoader(testDispatcher, testScope)

        sut.loadConfiguration(callback)
        advanceUntilIdle()

        val errorSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(errorSlot)) }

        assertTrue((errorSlot.captured as ConfigurationLoaderResult.Failure).error is JSONException)
    }

    @Test
    fun loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"

        val httpError = IOException("http error")
        coEvery {
            braintreeHttpClient.get(
                ofType(String::class),
                null,
                authorization
            )
        } throws httpError

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createConfigurationLoader(testDispatcher, testScope)

        sut.loadConfiguration(callback)
        advanceUntilIdle()

        val errorSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(errorSlot)) }

        assertEquals(
            (errorSlot.captured as ConfigurationLoaderResult.Failure).error.message,
            "Request for configuration has failed: http error"
        )
    }

    @Test
    fun loadConfiguration_whenInvalidToken_exception_is_returned() {
        every { merchantRepository.authorization } returns InvalidAuthorization("invalid", "token invalid")

        sut = createConfigurationLoader()
        sut.loadConfiguration(callback)

        val errorSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(errorSlot)) }

        assertEquals(
            (errorSlot.captured as ConfigurationLoaderResult.Failure).error.message,
            "Valid authorization required. See " +
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization " +
                "for more info."
        )
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

        sut = createConfigurationLoader()
        sut.loadConfiguration(callback)

        val successSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(successSlot)) }

        assertTrue(successSlot.captured is ConfigurationLoaderResult.Success)
    }

    @Test
    fun `when loadConfiguration is called and configuration is fetched from the API, analytics event is sent`() =
        runTest {
        every { authorization.configUrl } returns "https://example.com/config"
        every { merchantRepository.authorization } returns authorization

        val mockResponse = HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 10))
        coEvery {
            braintreeHttpClient.get(
                "https://example.com/config?configVersion=3",
                null,
                authorization
            )
        } returns mockResponse

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createConfigurationLoader(testDispatcher, testScope)

        sut.loadConfiguration(callback)
        advanceUntilIdle()

        verify {
            analyticsClient.sendEvent(
                eventName = CoreAnalytics.API_REQUEST_LATENCY,
                analyticsEventParams = AnalyticsEventParams(
                    startTime = 0,
                    endTime = 10,
                    endpoint = "/v1/configuration"
                ),
                sendImmediately = false
            )
        }

        val successSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(successSlot)) }

        assertTrue(successSlot.captured is ConfigurationLoaderResult.Success)
    }

    @Test
    fun loadConfiguration_onNullResponseBody_forwardsConfigurationExceptionToErrorResponseListener() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"

        val mockResponse = HttpResponse(null, HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.get(
                path = ofType(String::class),
                configuration = null,
                authorization = authorization
            )
        } returns mockResponse

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        sut = createConfigurationLoader(testDispatcher, testScope)

        sut.loadConfiguration(callback)
        advanceUntilIdle()

        val errorSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(errorSlot)) }

        val failure = errorSlot.captured as ConfigurationLoaderResult.Failure
        assertTrue(failure.error is ConfigurationException)
        assertEquals("Configuration responseBody is null", failure.error.message)
    }

    private fun createConfigurationLoader(
        testDispatcher: kotlinx.coroutines.CoroutineDispatcher? = null,
        testScope: kotlinx.coroutines.CoroutineScope? = null
    ) = ConfigurationLoader(
        httpClient = braintreeHttpClient,
        merchantRepository = merchantRepository,
        configurationCache = configurationCache,
        dispatcher = testDispatcher ?: kotlinx.coroutines.Dispatchers.Main,
        coroutineScope = testScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
        lazyAnalyticsClient = lazy { analyticsClient }
    )
}
