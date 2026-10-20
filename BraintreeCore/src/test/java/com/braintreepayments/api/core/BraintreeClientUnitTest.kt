package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.ManifestValidator
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.OkHttpRequest
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
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

    private val analyticsHttpClient = mockk<HttpClient>(relaxed = true)
    private val analyticsRequests = mutableListOf<OkHttpRequest>()
    private val authorizationA = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
    private val authorizationB = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY)
    private val configurationA = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
    private val configurationB = Configuration.fromJson(
        Fixtures.CONFIGURATION_WITH_ENVIRONMENT.replace("integration_merchant_id", "merchant_id_b")
    )

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
    fun `when configuration load succeeds, getConfiguration returns the loaded configuration`() = runTest(testDispatcher) {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val result = sut.getConfiguration()

        assertEquals(configuration, result)
    }

    @Test
    fun `when configuration load fails, getConfiguration throws the configuration loader error`() = runTest(testDispatcher) {
        val configFetchError = Exception("config fetch error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configFetchError)
            .build()

        val sut = createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        try {
            sut.getConfiguration()
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            assertEquals(configFetchError, e)
        }
    }

    @Test
    fun `when configuration loader throws JSONException during prefetch, BraintreeClient does not crash`() = runTest(testDispatcher) {
        val jsonException = JSONException("malformed JSON")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(jsonException)
            .build()

        createBraintreeClient(
            configurationLoader = configurationLoader,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        advanceUntilIdle()
    }

    @Test
    fun `when authorization is invalid, getConfiguration throws the auth error`() = runTest(testDispatcher) {
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
            sut.getConfiguration()
            fail("Expected exception to be thrown")
        } catch (e: BraintreeException) {
            assertEquals(expectedAuthException.message, e.message)
        }
    }

    @Test
    fun `when getConfiguration succeeds, sendGET forwards the request to httpClient`() = runTest(testDispatcher) {
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
    fun `when getConfiguration fails, sendGET throws the configuration error`() = runTest(testDispatcher) {
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
    fun `when authorization is invalid, sendGET throws the auth error`() = runTest(testDispatcher) {
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
    fun `when getConfiguration succeeds, sendPOST forwards the request to httpClient`() = runTest(testDispatcher) {
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
    fun `when getConfiguration fails, sendPOST throws the configuration error`() = runTest(testDispatcher) {
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
    fun `when sendPOST is called without additionalHeaders, an empty map is used`() = runTest(testDispatcher) {
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
    fun `when sendPOST is called with additionalHeaders, they are forwarded to httpClient post`() = runTest(testDispatcher) {
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
    fun `when authorization is invalid, sendPOST throws the auth error`() = runTest(testDispatcher) {
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
    fun `when getConfiguration succeeds, sendGraphQLPOST forwards the request to graphQLClient`() = runTest(testDispatcher) {
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
    fun `when getConfiguration fails, sendGraphQLPOST throws the configuration error`() = runTest(testDispatcher) {
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
    fun `when authorization is invalid, sendGraphQLPOST throws the auth error`() = runTest(testDispatcher) {
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
    fun `when two clients are created with different authorizations, each client sends requests and loads configuration with its own authorization`() = runTest(testDispatcher) {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val tokenizationKeyAuth = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val sharedMerchantRepository = MerchantRepository()

        val clientOne = createBraintreeClient(
            configurationLoader = configurationLoader,
            merchantRepository = sharedMerchantRepository,
            testDispatcher = testDispatcher,
            testScope = testScope
        )
        val clientTwo = createBraintreeClient(
            auth = tokenizationKeyAuth,
            configurationLoader = configurationLoader,
            merchantRepository = sharedMerchantRepository,
            testDispatcher = testDispatcher,
            testScope = testScope
        )

        val mockResponse = HttpResponse(body = "response-body", timing = HttpResponseTiming(0, 0))
        coEvery { braintreeHttpClient.get(any(), any(), any()) } returns mockResponse
        coEvery { braintreeHttpClient.post(any(), any(), any(), any(), any()) } returns mockResponse
        coEvery { braintreeGraphQLClient.post(any(), any(), any()) } returns mockResponse

        clientOne.sendGET("sample-url")
        clientOne.sendPOST("sample-url", "{}")
        clientOne.sendGraphQLPOST(JSONObject())

        clientTwo.sendGET("sample-url")
        clientTwo.sendPOST("sample-url", "{}")
        clientTwo.sendGraphQLPOST(JSONObject())

        advanceUntilIdle()

        coVerify { braintreeHttpClient.get("sample-url", configuration, authorization) }
        coVerify { braintreeHttpClient.get("sample-url", configuration, tokenizationKeyAuth) }
        coVerify {
            braintreeHttpClient.post(
                path = "sample-url",
                data = "{}",
                configuration = configuration,
                authorization = authorization,
                additionalHeaders = emptyMap()
            )
        }
        coVerify {
            braintreeHttpClient.post(
                path = "sample-url",
                data = "{}",
                configuration = configuration,
                authorization = tokenizationKeyAuth,
                additionalHeaders = emptyMap()
            )
        }
        coVerify { braintreeGraphQLClient.post("{}", configuration, authorization) }
        coVerify { braintreeGraphQLClient.post("{}", configuration, tokenizationKeyAuth) }
        coVerify { configurationLoader.loadConfiguration(authorization, true) }
        coVerify { configurationLoader.loadConfiguration(tokenizationKeyAuth, true) }
        assertSame(authorization, clientOne.authorization)
        assertSame(tokenizationKeyAuth, clientTwo.authorization)
    }

    @Test
    @Throws(JSONException::class)
    fun `when sendAnalyticsEvent is called, event is sent to analyticsClient`() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val sut = createBraintreeClient(configurationLoader)
        sut.sendAnalyticsEvent("event.started")

        verify {
            analyticsClient.sendEvent("event.started", authorization, AnalyticsEventParams(), true)
        }
    }

    @Test
    fun `when getManifestActivityInfo is called, invocation is forwarded to manifestValidator`() {
        val activityInfo = ActivityInfo()
        every {
            manifestValidator.getActivityInfo(applicationContext, FragmentActivity::class.java)
        } returns activityInfo

        val sut = createBraintreeClient(configurationLoader)
        assertSame(activityInfo, sut.getManifestActivityInfo(FragmentActivity::class.java))
    }

    @Test
    fun `when no returnUrlScheme is provided in constructor, getReturnUrlScheme is based on application id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sut = BraintreeClient(context, authorization.toString())
        assertEquals("com.braintreepayments.api.core.test.braintree", sut.getReturnUrlScheme())
    }

    @Test
    fun `when a returnUrlScheme is provided in constructor, getReturnUrlScheme returns it`() {
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
    fun `when another client is created later, getReturnUrlScheme keeps the scheme from its own constructor`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = BraintreeClient(
            context = context,
            authorization = authorization.toString(),
            returnUrlScheme = "custom-url-scheme"
        )
        BraintreeClient(context, authorization.toString())

        assertEquals("custom-url-scheme", first.getReturnUrlScheme())
    }

    @Test
    @Throws(JSONException::class)
    fun `when reportCrash is called, crash is reported via analyticsClient`() = runTest(testDispatcher) {
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

        verify { analyticsClient.reportCrash(authorization) }
    }

    @Test
    fun `when client A queues an event and sends immediately after client B is created, the post uses A's credentials`() =
        runTest(testDispatcher) {
            val configurationLoader = MockkConfigurationLoaderBuilder()
                .configurationsByAuthorization(
                    mapOf(authorizationA to configurationA, authorizationB to configurationB)
                )
                .build()
            val clients = createClientsWithRealAnalytics(
                configurationLoader,
                listOf(authorizationA, authorizationB)
            )
            val clientA = clients[0]

            clientA.sendAnalyticsEvent("a.queued.event", sendImmediately = false)
            clientA.sendAnalyticsEvent("a.immediate.event")
            advanceUntilIdle()

            assertEquals(1, analyticsRequests.size)
            assertAnalyticsPost(
                request = analyticsRequests[0],
                authorization = authorizationA,
                merchantId = configurationA.merchantId,
                eventNames = listOf("a.queued.event", "a.immediate.event")
            )
        }

    @Test
    fun `when client A queues an event and client B sends immediately, each post carries only its own client's events`() =
        runTest(testDispatcher) {
            val configurationLoader = MockkConfigurationLoaderBuilder()
                .configurationsByAuthorization(
                    mapOf(authorizationA to configurationA, authorizationB to configurationB)
                )
                .build()
            val (clientA, clientB) = createClientsWithRealAnalytics(
                configurationLoader,
                listOf(authorizationA, authorizationB)
            )

            clientA.sendAnalyticsEvent("a.queued.event", sendImmediately = false)
            clientB.sendAnalyticsEvent("b.immediate.event")
            advanceUntilIdle()

            assertEquals(2, analyticsRequests.size)
            assertAnalyticsPost(
                request = analyticsRequests[0],
                authorization = authorizationB,
                merchantId = configurationB.merchantId,
                eventNames = listOf("b.immediate.event")
            )
            assertAnalyticsPost(
                request = analyticsRequests[1],
                authorization = authorizationA,
                merchantId = configurationA.merchantId,
                eventNames = listOf("a.queued.event")
            )
        }

    @Test
    fun `when client A reports a crash after client B is created, the crash post uses A's credentials`() =
        runTest(testDispatcher) {
            val configurationLoader = MockkConfigurationLoaderBuilder()
                .configurationsByAuthorization(
                    mapOf(authorizationA to configurationA, authorizationB to configurationB)
                )
                .build()
            val clients = createClientsWithRealAnalytics(
                configurationLoader,
                listOf(authorizationA, authorizationB)
            )
            val clientA = clients[0]

            clientA.reportCrash()
            advanceUntilIdle()

            assertEquals(1, analyticsRequests.size)
            assertAnalyticsPost(
                request = analyticsRequests[0],
                authorization = authorizationA,
                merchantId = configurationA.merchantId,
                eventNames = listOf("crash")
            )
        }

    @Test
    fun `when client A's configuration load fails, queued events remain queued until a later successful send`() =
        runTest(testDispatcher) {
            val configurationLoader = MockkConfigurationLoaderBuilder()
                .configurationError(Exception("config fetch error"))
                .build()
            val clientA = createClientsWithRealAnalytics(
                configurationLoader,
                listOf(authorizationA)
            )[0]

            clientA.sendAnalyticsEvent("a.queued.event", sendImmediately = false)
            clientA.sendAnalyticsEvent("a.failed.immediate.event")
            advanceUntilIdle()
            assertEquals(0, analyticsRequests.size)

            coEvery { configurationLoader.loadConfiguration(authorizationA) } returns
                ConfigurationLoaderResult.Success(configurationA)
            clientA.sendAnalyticsEvent("a.successful.immediate.event")
            advanceUntilIdle()

            assertEquals(1, analyticsRequests.size)
            assertAnalyticsPost(
                request = analyticsRequests[0],
                authorization = authorizationA,
                merchantId = configurationA.merchantId,
                eventNames = listOf("a.queued.event", "a.successful.immediate.event")
            )
        }

    @Test
    fun `when two clients with separate analytics clients share one repository, each event is delivered once with its own client's credentials`() =
        runTest(testDispatcher) {
            val clientTokenAuthorization = authorization
            val configurationLoader = MockkConfigurationLoaderBuilder()
                .configurationsByAuthorization(
                    mapOf(authorizationA to configurationA, clientTokenAuthorization to configurationB)
                )
                .build()
            val (clientA, clientB) = createClientsWithSeparateAnalyticsClients(
                configurationLoader,
                listOf(authorizationA, clientTokenAuthorization)
            )

            clientA.sendAnalyticsEvent("a.queued.event", sendImmediately = false)
            clientB.sendAnalyticsEvent("b.queued.event", sendImmediately = false)
            clientA.sendAnalyticsEvent("a.immediate.event")
            clientB.sendAnalyticsEvent("b.immediate.event")
            advanceUntilIdle()

            val posts = analyticsRequests.map { request ->
                val container = JSONObject((request.method as Method.Post).body)
                    .getJSONArray("events").getJSONObject(0)
                val batchParams = container.getJSONObject("batch_params")
                val eventParams = container.getJSONArray("event_params")
                val names = (0 until eventParams.length()).map { eventParams.getJSONObject(it).getString("event_name") }
                val isTokenizationKeyPost = request.headers.containsKey("Client-Key")
                if (isTokenizationKeyPost) {
                    assertEquals(authorizationA.bearer, request.headers["Client-Key"])
                    assertEquals(authorizationA.bearer, batchParams.getString("tokenization_key"))
                    assertEquals(configurationA.merchantId, batchParams.getString("merchant_id"))
                } else {
                    assertEquals("Bearer ${clientTokenAuthorization.bearer}", request.headers["Authorization"])
                    assertEquals(clientTokenAuthorization.bearer, batchParams.getString("authorization_fingerprint"))
                    assertEquals(configurationB.merchantId, batchParams.getString("merchant_id"))
                }
                assertTrue(names.all { it.startsWith(if (isTokenizationKeyPost) "a." else "b.") })
                names
            }

            assertEquals(
                listOf("a.immediate.event", "a.queued.event", "b.immediate.event", "b.queued.event"),
                posts.flatten().sorted()
            )
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
    fun `when BraintreeClient is initialized and appLinkReturnUri is null, appLinkReturnUri is not set on the MerchantRepository`() {
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

    private fun createClientsWithRealAnalytics(
        configurationLoader: ConfigurationLoader,
        authorizations: List<Authorization>
    ): List<BraintreeClient> {
        coEvery { analyticsHttpClient.sendRequest(any()) } coAnswers {
            analyticsRequests.add(firstArg())
            HttpResponse("{}", HttpResponseTiming(0, 0))
        }

        val analyticsMerchantRepository = MerchantRepository()
        val analyticsParamRepository = AnalyticsParamRepository()
        val analyticsApi = AnalyticsApi(
            httpClient = BraintreeHttpClient(analyticsHttpClient),
            deviceInspector = DeviceInspector(applicationContext),
            analyticsParamRepository = analyticsParamRepository,
            merchantRepository = analyticsMerchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = testScope
        )
        val realAnalyticsClient = AnalyticsClient(
            analyticsApi = analyticsApi,
            analyticsParamRepository = analyticsParamRepository,
            analyticsEventRepository = AnalyticsEventRepository(),
            configurationLoader = configurationLoader,
            dispatcher = testDispatcher,
            coroutineScope = testScope
        )
        return authorizations.map { authorization ->
            BraintreeClient(
                applicationContext = applicationContext,
                integrationType = IntegrationType.CUSTOM,
                authorization = authorization,
                returnUrlScheme = "sample-return-url-scheme",
                appLinkReturnUri = null,
                httpClient = braintreeHttpClient,
                graphQLClient = braintreeGraphQLClient,
                configurationLoader = configurationLoader,
                manifestValidator = manifestValidator,
                merchantRepository = analyticsMerchantRepository,
                analyticsClient = realAnalyticsClient,
                dispatcher = testDispatcher,
                coroutineScope = testScope
            )
        }
    }

    private fun createClientsWithSeparateAnalyticsClients(
        configurationLoader: ConfigurationLoader,
        authorizations: List<Authorization>
    ): List<BraintreeClient> {
        coEvery { analyticsHttpClient.sendRequest(any()) } coAnswers {
            analyticsRequests.add(firstArg())
            HttpResponse("{}", HttpResponseTiming(0, 0))
        }

        val analyticsMerchantRepository = MerchantRepository()
        val analyticsParamRepository = AnalyticsParamRepository()
        val sharedEventRepository = AnalyticsEventRepository()
        val analyticsApi = AnalyticsApi(
            httpClient = BraintreeHttpClient(analyticsHttpClient),
            deviceInspector = DeviceInspector(applicationContext),
            analyticsParamRepository = analyticsParamRepository,
            merchantRepository = analyticsMerchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = CoroutineScope(testDispatcher)
        )
        return authorizations.map { authorization ->
            BraintreeClient(
                applicationContext = applicationContext,
                integrationType = IntegrationType.CUSTOM,
                authorization = authorization,
                returnUrlScheme = "sample-return-url-scheme",
                appLinkReturnUri = null,
                httpClient = braintreeHttpClient,
                graphQLClient = braintreeGraphQLClient,
                configurationLoader = configurationLoader,
                manifestValidator = manifestValidator,
                merchantRepository = analyticsMerchantRepository,
                analyticsClient = AnalyticsClient(
                    analyticsApi = analyticsApi,
                    analyticsParamRepository = analyticsParamRepository,
                    analyticsEventRepository = sharedEventRepository,
                    configurationLoader = configurationLoader,
                    dispatcher = testDispatcher,
                    coroutineScope = CoroutineScope(testDispatcher)
                ),
                dispatcher = testDispatcher,
                coroutineScope = testScope
            )
        }
    }

    private fun assertAnalyticsPost(
        request: OkHttpRequest,
        authorization: Authorization,
        merchantId: String,
        eventNames: List<String>
    ) {
        assertEquals("https://api-m.paypal.com/v1/tracking/batch/events", request.url)
        assertTrue(request.method is Method.Post)
        assertEquals(authorization.bearer, request.headers["Client-Key"])
        val eventContainer = JSONObject((request.method as Method.Post).body)
            .getJSONArray("events")
            .getJSONObject(0)
        assertEquals(authorization.bearer, eventContainer.getJSONObject("batch_params").getString("tokenization_key"))
        assertEquals(merchantId, eventContainer.getJSONObject("batch_params").getString("merchant_id"))
        val eventParams = eventContainer.getJSONArray("event_params")
        val actualEventNames = (0 until eventParams.length()).map {
            eventParams.getJSONObject(it).getString("event_name")
        }
        assertEquals(eventNames, actualEventNames)
    }
}
