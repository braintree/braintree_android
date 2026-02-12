package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.ManifestValidator
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BraintreeClientUnitTest {

    private lateinit var authorization: Authorization
    private lateinit var context: Context
    private lateinit var applicationContext: Context
    private lateinit var braintreeHttpClient: BraintreeHttpClient
    private lateinit var braintreeGraphQLClient: BraintreeGraphQLClient
    private lateinit var configurationLoader: ConfigurationLoader
    private lateinit var analyticsClient: AnalyticsClient
    private lateinit var manifestValidator: ManifestValidator
    private lateinit var browserSwitchClient: BrowserSwitchClient
    private lateinit var expectedAuthException: BraintreeException
    private lateinit var merchantRepository: MerchantRepository
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun beforeEach() {
        authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)
        context = mockk(relaxed = true)
        applicationContext = ApplicationProvider.getApplicationContext()

        braintreeHttpClient = mockk(relaxed = true)
        braintreeGraphQLClient = mockk(relaxed = true)
        configurationLoader = mockk(relaxed = true)

        analyticsClient = mockk(relaxed = true)
        manifestValidator = mockk(relaxed = true)
        browserSwitchClient = mockk(relaxed = true)
        merchantRepository = mockk(relaxed = true)

        testScope = TestScope(testDispatcher)

        val clientSDKSetupURL =
            "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
        val message = "Valid authorization required. See $clientSDKSetupURL for more info."
        expectedAuthException = BraintreeException(message)

        every { context.applicationContext } returns applicationContext
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    @Throws(JSONException::class)
    fun configuration_onAuthorizationAndConfigurationLoadSuccess_forwardsResult() = runTest(testDispatcher) {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )
        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)

        advanceUntilIdle()

        verify { callback.onResult(configuration, null) }
    }

    @Test
    fun configuration_forwardsConfigurationLoaderError() = runTest(testDispatcher) {
        val configFetchError = Exception("config fetch error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configFetchError)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)

        advanceUntilIdle()

        verify { callback.onResult(null, configFetchError) }
    }

    @Test
    fun configuration_whenInvalidAuth_callsBackAuthError() = runTest(testDispatcher) {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(expectedAuthException)
            .build()

        val sut = createBraintreeClient(
            auth = Authorization.fromString("invalid-auth-string"),
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)
        advanceUntilIdle()

        val authErrorSlot = slot<BraintreeException>()
        verify { callback.onResult(isNull(), capture(authErrorSlot)) }

        assertEquals(expectedAuthException.message, authErrorSlot.captured.message)
    }

    @Test
    fun sendGET_onGetConfigurationSuccess_forwardsRequestToHttpClient() = runTest(testDispatcher) {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val mockResponse = HttpResponse(body = "response-body", timing = HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.get("sample-url", configuration, authorization)
        } returns mockResponse

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val responseBody = sut.sendGET("sample-url")
        assertEquals("response-body", responseBody)

        coVerify {
            braintreeHttpClient.get("sample-url", configuration, authorization)
        }
    }

    @Test
    fun sendGET_onGetConfigurationFailure_forwardsErrorToCallback() = runTest(testDispatcher) {
        val configError = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configError)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        try {
            sut.sendGET("sample-url")
            fail("Must throw an exception")
        } catch (e: Exception) {
            assertEquals("configuration error", e.message)
        }
    }

    @Test
    fun sendGET_whenInvalidAuth_callsBackAuthError() = runTest(testDispatcher) {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(expectedAuthException)
            .build()

        val sut = createBraintreeClient(
            auth = Authorization.fromString("invalid-auth-string"),
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        try {
            sut.sendGET("sample-url")
            fail("Must throw an exception")
        } catch (e: BraintreeException) {
            assertEquals(expectedAuthException.message, e.message)
        }
    }

    @Test
    fun sendPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() = runTest(testDispatcher) {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.post(
                path = "sample-url",
                data = "{}",
                configuration = configuration,
                authorization = authorization,
                additionalHeaders = emptyMap()
            )
        } returns mockResponse

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val result = sut.sendPOST("sample-url", "{}", emptyMap())

        assertEquals("{}", result)
        coVerify {
            braintreeHttpClient.post(
                path = "sample-url",
                data = "{}",
                configuration = configuration,
                authorization = authorization,
                additionalHeaders = emptyMap()
            )
        }
    }

    @Test
    fun sendPOST_onGetConfigurationFailure_throwsException() = runTest(testDispatcher) {
        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        try {
            sut.sendPOST("sample-url", "{}", emptyMap())
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            assertEquals("configuration error", e.message)
        }
    }

    @Test
    fun `sendPOST defaults additionalHeaders to an empty map`() = runTest(testDispatcher) {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.post(
                path = any(),
                data = any(),
                configuration = any(),
                authorization = any(),
                additionalHeaders = emptyMap()
            )
        } returns mockResponse

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val result = sut.sendPOST(
            url = "sample-url",
            data = "{}"
        )
        advanceUntilIdle()

        assertEquals("{}", result)
        coVerify {
            braintreeHttpClient.post(
                path = any(),
                data = any(),
                configuration = any(),
                authorization = any(),
                additionalHeaders = emptyMap()
            )
        }
    }

    @Test
    fun `sendPOST sends additionalHeaders to httpClient post`() = runTest(testDispatcher) {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()
        val headers = mapOf("name" to "value")

        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery {
            braintreeHttpClient.post(
                path = any(),
                data = any(),
                configuration = any(),
                authorization = any(),
                additionalHeaders = headers
            )
        } returns mockResponse

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val result = sut.sendPOST(
            url = "sample-url",
            data = "{}",
            additionalHeaders = headers
        )
        advanceUntilIdle()

        assertEquals("{}", result)
        coVerify {
            braintreeHttpClient.post(
                path = any(),
                data = any(),
                configuration = any(),
                authorization = any(),
                additionalHeaders = headers
            )
        }
    }

    @Test
    fun sendPOST_whenInvalidAuth_callsBackAuthError() = runTest(testDispatcher) {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(expectedAuthException)
            .build()

        val sut = createBraintreeClient(
            auth = Authorization.fromString("invalid-auth-string"),
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        try {
            sut.sendPOST("sample-url", "{}", emptyMap())
            fail("Expected exception to be thrown")
        } catch (e: BraintreeException) {
            assertEquals(expectedAuthException.message, e.message)
        }
    }

    @Test
    fun sendGraphQLPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() = runTest(testDispatcher) {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val mockResponse = HttpResponse(body = "{}", timing = HttpResponseTiming(0, 0))
        coEvery {
            braintreeGraphQLClient.post(
                "{}",
                configuration,
                authorization
            )
        } returns mockResponse

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val response = sut.sendGraphQLPOST(JSONObject())
        advanceUntilIdle()

        coVerify {
            braintreeGraphQLClient.post(
                "{}",
                configuration,
                authorization
            )
        }
        assertEquals("{}", response)
    }

    @Test
    fun sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() = runTest(testDispatcher) {
        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        try {
            sut.sendGraphQLPOST(JSONObject())
            advanceUntilIdle()
            fail("Must throw an exception")
        } catch (e: Exception) {
            assertEquals("configuration error", e.message)
        }
    }

    @Test
    fun sendGraphQLPOST_whenInvalidAuth_callsBackAuthError() = runTest(testDispatcher) {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(expectedAuthException)
            .build()

        val sut = createBraintreeClient(
            auth = Authorization.fromString("invalid-auth-string"),
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        try {
            sut.sendGraphQLPOST(JSONObject())
            fail("Must throw an exception")
        } catch (e: BraintreeException) {
            assertEquals(expectedAuthException.message, e.message)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun sendAnalyticsEvent_sendsEventToAnalyticsClient() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val sut = createBraintreeClient(configurationLoader)
        sut.sendAnalyticsEvent("event.started")

        verify {
            analyticsClient.sendEvent("event.started")
        }
    }

    @Test
    fun manifestActivityInfo_forwardsInvocationToManifestValidator() {
        val activityInfo = ActivityInfo()
        every {
            manifestValidator.getActivityInfo(applicationContext, FragmentActivity::class.java)
        } returns activityInfo

        val sut = createBraintreeClient(configurationLoader)
        assertSame(activityInfo, sut.getManifestActivityInfo(FragmentActivity::class.java))
    }

    @Test
    fun returnUrlScheme_returnsUrlSchemeBasedOnApplicationIdByDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sut = BraintreeClient(context, authorization.toString())
        assertEquals("com.braintreepayments.api.core.test.braintree", sut.getReturnUrlScheme())
    }

    @Test
    fun returnUrlScheme_returnsUrlSchemeDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val returnUrlScheme = "custom-url-scheme"
        val sut = BraintreeClient(
            context = context,
            authorization = authorization.toString(),
            returnUrlScheme = returnUrlScheme
        )
        assertEquals("custom-url-scheme", sut.getReturnUrlScheme())
    }

    @Test
    @Throws(JSONException::class)
    fun reportCrash_reportsCrashViaAnalyticsClient() = runTest(testDispatcher) {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )
        sut.reportCrash()
        advanceUntilIdle()

        coVerify {
            configurationLoader.loadConfiguration()
        }

        verify { analyticsClient.reportCrash(any()) }
    }

    @Test
    fun `when BraintreeClient is initialized, merchantRepository properties are set`() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        createBraintreeClient(configurationLoader = configurationLoader, merchantRepository = merchantRepository)
        verify { merchantRepository.returnUrlScheme = "sample-return-url-scheme" }
        verify { merchantRepository.applicationContext = applicationContext }
        verify { merchantRepository.authorization = authorization }
        verify { merchantRepository.appLinkReturnUri = Uri.parse("https://example.com") }
        verify { merchantRepository.integrationType = IntegrationType.CUSTOM }
    }

    @Test
    fun `when BraintreeClient is initialized and appLinkReturnUri is null, it is not set on the MerchantRepository`() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        createBraintreeClient(configurationLoader, appLinkReturnUri = null, merchantRepository = merchantRepository)
        verify(exactly = 0) { merchantRepository.appLinkReturnUri = null }
    }

    private fun createBraintreeClient(
        configurationLoader: ConfigurationLoader = mockk(),
        appLinkReturnUri: Uri? = Uri.parse("https://example.com"),
        merchantRepository: MerchantRepository = MerchantRepository.instance,
        testDispatcher: kotlinx.coroutines.CoroutineDispatcher? = null,
        testScope: kotlinx.coroutines.CoroutineScope? = null,
        auth: Authorization? = null
    ) = BraintreeClient(
        applicationContext = applicationContext,
        integrationType = IntegrationType.CUSTOM,
        authorization = auth ?: authorization,
        returnUrlScheme = "sample-return-url-scheme",
        appLinkReturnUri = appLinkReturnUri,
        httpClient = braintreeHttpClient,
        graphQLClient = braintreeGraphQLClient,
        analyticsClient = analyticsClient,
        manifestValidator = manifestValidator,
        configurationLoader = configurationLoader,
        merchantRepository = merchantRepository,
        dispatcher = testDispatcher ?: kotlinx.coroutines.Dispatchers.Main,
        coroutineScope = testScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    )
}
