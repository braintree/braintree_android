package com.braintreepayments.api.core

import android.util.Base64
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val authorization: Authorization = mockk(relaxed = true)
    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)

    private lateinit var sut: ConfigurationLoader

    @Before
    fun setUp() {
        every { merchantRepository.authorization } returns authorization
    }

    @Test
    fun `when httpClient get returns a valid config response, loadConfiguration returns Success`() = runTest {
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

        sut = createConfigurationLoader()
        val configResult = sut.loadConfiguration()

        assertTrue(configResult is ConfigurationLoaderResult.Success)
    }

    @Test
    fun `when loadConfiguration fetches a new configuration, the configuration is saved to the cache`() = runTest {
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

        sut = createConfigurationLoader()
        sut.loadConfiguration()

        val cacheKey = Base64.encodeToString(
            "https://example.com/config?configVersion=3bearer".toByteArray(),
            0
        )

        verify {
            configurationCache.saveConfiguration(ofType(Configuration::class), cacheKey)
        }
    }

    @Test
    fun `when response body is not valid json, loadConfiguration returns a Failure with a JSONException`() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"

        val mockResponse = HttpResponse("not json", HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.get(
                ofType(String::class),
                null,
                authorization
            )
        } returns mockResponse

        sut = createConfigurationLoader()
        val configResult = sut.loadConfiguration()

        assertTrue((configResult as ConfigurationLoaderResult.Failure).error is JSONException)
    }

    @Test
    fun `when httpClient get throws an IOException, loadConfiguration returns a Failure wrapping the error message`() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"

        val httpError = IOException("http error")
        coEvery {
            braintreeHttpClient.get(
                ofType(String::class),
                null,
                authorization
            )
        } throws httpError

        sut = createConfigurationLoader()
        val configResult = sut.loadConfiguration()

        assertEquals(
            (configResult as ConfigurationLoaderResult.Failure).error.message,
            "Request for configuration has failed: http error"
        )
    }

    @Test
    fun `when authorization is invalid, loadConfiguration returns a Failure with the auth error message`() = runTest {
        every { merchantRepository.authorization } returns InvalidAuthorization("invalid", "token invalid")

        sut = createConfigurationLoader()
        val configResult = sut.loadConfiguration()

        assertEquals(
            (configResult as ConfigurationLoaderResult.Failure).error.message,
            "Valid authorization required. See " +
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization " +
                "for more info."
        )
    }

    @Test
    fun `when a cached configuration is available, loadConfiguration loads it from the cache`() = runTest {
        val cacheKey = Base64.encodeToString(
            "https://example.com/config?configVersion=3bearer".toByteArray(),
            0
        )
        every { authorization.configUrl } returns "https://example.com/config"
        every { authorization.bearer } returns "bearer"
        every { configurationCache.getConfiguration(cacheKey) } returns Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN

        sut = createConfigurationLoader()
        val configResult = sut.loadConfiguration()

        assertTrue(configResult is ConfigurationLoaderResult.Success)
    }

    @Test
    fun `when configuration is fetched from the API, loadConfiguration sends an API_REQUEST_LATENCY analytics event`() =
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

        sut = createConfigurationLoader()
        val configResult = sut.loadConfiguration()

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

        assertTrue(configResult is ConfigurationLoaderResult.Success)
    }

    @Test
    fun `when response body is null, loadConfiguration returns a Failure with a ConfigurationException`() = runTest {
        every { authorization.configUrl } returns "https://example.com/config"

        val mockResponse = HttpResponse(null, HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.get(
                path = ofType(String::class),
                configuration = null,
                authorization = authorization
            )
        } returns mockResponse

        sut = createConfigurationLoader()
        val configResult = sut.loadConfiguration()

        val failure = configResult as ConfigurationLoaderResult.Failure
        assertTrue(failure.error is ConfigurationException)
        assertEquals("Configuration responseBody is null", failure.error.message)
    }

    private fun createConfigurationLoader() = ConfigurationLoader(
        httpClient = braintreeHttpClient,
        merchantRepository = merchantRepository,
        configurationCache = configurationCache,
        lazyAnalyticsClient = lazy { analyticsClient }
    )
}
