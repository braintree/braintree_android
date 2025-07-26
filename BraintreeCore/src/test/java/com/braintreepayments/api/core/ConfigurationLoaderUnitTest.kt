package com.braintreepayments.api.core

import android.util.Base64
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

        sut = ConfigurationLoader(
            httpClient = braintreeHttpClient,
            merchantRepository = merchantRepository,
            configurationCache = configurationCache,
            lazyAnalyticsClient = lazy { analyticsClient }
        )
    }

    @Test
    fun loadConfiguration_loadsConfigurationForTheCurrentEnvironment() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { merchantRepository.authorization } returns authorization

        sut.loadConfiguration(callback)

        val expectedConfigUrl = "https://example.com/config?configVersion=3"
        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.get(
                expectedConfigUrl,
                null,
                authorization,
                HttpClient.RetryStrategy.RETRY_MAX_3_TIMES,
                capture(callbackSlot)
            )
        }

        val httpResponseCallback = callbackSlot.captured
        httpResponseCallback.onResult(
            HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 0)), null
        )

        val successSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(successSlot)) }

        assertTrue { successSlot.captured is ConfigurationLoaderResult.Success }
    }

    @Test
    fun loadConfiguration_savesFetchedConfigurationToCache() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { authorization.bearer } returns "bearer"

        sut.loadConfiguration(callback)

        val expectedConfigUrl = "https://example.com/config?configVersion=3"
        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.get(
                expectedConfigUrl,
                null,
                authorization,
                HttpClient.RetryStrategy.RETRY_MAX_3_TIMES,
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

        sut.loadConfiguration(callback)

        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.get(
                ofType(String::class),
                null,
                authorization,
                HttpClient.RetryStrategy.RETRY_MAX_3_TIMES,
                capture(callbackSlot)
            )
        }
        val httpResponseCallback = callbackSlot.captured
        httpResponseCallback.onResult(HttpResponse("not json", HttpResponseTiming(0, 0)), null)

        val errorSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(errorSlot)) }

        assertTrue { (errorSlot.captured as ConfigurationLoaderResult.Failure).error is JSONException }
    }

    @Test
    fun loadConfiguration_onHttpError_forwardsExceptionToErrorResponseListener() {
        every { authorization.configUrl } returns "https://example.com/config"

        sut.loadConfiguration(callback)

        val callbackSlot = slot<NetworkResponseCallback>()

        verify {
            braintreeHttpClient.get(
                ofType(String::class),
                null,
                authorization,
                HttpClient.RetryStrategy.RETRY_MAX_3_TIMES,
                capture(callbackSlot)
            )
        }

        val httpResponseCallback = callbackSlot.captured
        val httpError = Exception("http error")
        httpResponseCallback.onResult(null, httpError)
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

        sut.loadConfiguration(callback)

        verify(exactly = 0) {
            braintreeHttpClient.get(
                ofType(String::class),
                null,
                authorization,
                any(),
                ofType(NetworkResponseCallback::class)
            )
        }

        val successSlot = slot<ConfigurationLoaderResult>()
        verify { callback.onResult(capture(successSlot)) }

        assertTrue { successSlot.captured is ConfigurationLoaderResult.Success }
    }

    @Test
    fun `when loadConfiguration is called and configuration is fetched from the API, analytics event is sent`() {
        every { authorization.configUrl } returns "https://example.com/config"
        every { merchantRepository.authorization } returns authorization

        sut.loadConfiguration(callback)

        val expectedConfigUrl = "https://example.com/config?configVersion=3"
        val callbackSlot = slot<NetworkResponseCallback>()
        verify {
            braintreeHttpClient.get(
                expectedConfigUrl,
                null,
                authorization,
                HttpClient.RetryStrategy.RETRY_MAX_3_TIMES,
                capture(callbackSlot)
            )
        }

        val httpResponseCallback = callbackSlot.captured
        httpResponseCallback.onResult(
            HttpResponse(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN, HttpResponseTiming(0, 10)), null
        )

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

        assertTrue { successSlot.captured is ConfigurationLoaderResult.Success }
    }
}
