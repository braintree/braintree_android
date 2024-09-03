package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.sharedutils.HttpMethod
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import com.braintreepayments.api.sharedutils.ManifestValidator
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockThreadScheduler
import io.mockk.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    private lateinit var threadScheduler: MockThreadScheduler

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
        threadScheduler = MockThreadScheduler()

        val clientSDKSetupURL =
            "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
        val message = "Valid authorization required. See $clientSDKSetupURL for more info."
        expectedAuthException = BraintreeException(message)

        every { context.applicationContext } returns applicationContext
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun constructor_usesSessionIdFromParams() {
        val params = BraintreeOptions(
            context = context, sessionId = "session-id", authorization =
            authorization
        )
        val sut = BraintreeClient(params)
        assertEquals("session-id", sut.sessionId)
    }

    @Test
    fun constructor_setsSessionIdFromUUIDHelperIfSessionIdNotIncluded() {
        val uuidRegex = """[a-fA-F0-9]{32}""".toRegex()

        val sut =
            BraintreeClient(BraintreeOptions(context = context, authorization = authorization))
        assertTrue(uuidRegex.matches(sut.sessionId))
    }

    @Test
    @Throws(JSONException::class)
    fun configuration_onAuthorizationAndConfigurationLoadSuccess_forwardsResult() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)

        verify { callback.onResult(configuration, null) }
    }

    @Test
    fun configuration_forwardsConfigurationLoaderError() {
        val configFetchError = Exception("config fetch error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configFetchError)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)

        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)

        verify { callback.onResult(null, configFetchError) }
    }

    @Test
    fun configuration_whenInvalidAuth_callsBackAuthError() {
        val sut = BraintreeClient(context, "invalid-auth-string")

        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)

        val authErrorSlot = slot<BraintreeException>()
        verify { callback.onResult(isNull(), capture(authErrorSlot)) }

        assertEquals(expectedAuthException.message, authErrorSlot.captured.message)
    }

    @Test
    fun sendGET_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendGET("sample-url", httpResponseCallback)
        threadScheduler.flushBackgroundThread()
        threadScheduler.flushMainThread()

        val httpRequestSlot = slot<InternalHttpRequest>()
        verify {
            braintreeHttpClient.sendRequestSync(
                capture(httpRequestSlot),
                configuration,
                authorization,
            )
        }

        assertEquals(HttpMethod.GET, httpRequestSlot.captured.method)
        assertEquals("sample-url", httpRequestSlot.captured.path)
        verify { httpResponseCallback.onResult(any(), any()) }
    }

    @Test
    fun sendGET_onGetConfigurationFailure_forwardsErrorToCallback() {
        val configError = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configError)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendGET("sample-url", httpResponseCallback)

        verify { httpResponseCallback.onResult(null, configError) }
    }

    @Test
    fun sendGET_whenInvalidAuth_callsBackAuthError() {
        val sut = BraintreeClient(context, "invalid-auth-string")

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendGET("sample-url", httpResponseCallback)

        val authErrorSlot = slot<BraintreeException>()
        verify { httpResponseCallback.onResult(isNull(), capture(authErrorSlot)) }

        assertEquals(expectedAuthException.message, authErrorSlot.captured.message)
    }

    @Test
    fun sendPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendPOST("sample-url", "{}", emptyMap(), httpResponseCallback)
        threadScheduler.flushBackgroundThread()
        threadScheduler.flushMainThread()

        val httpRequestSlot = slot<InternalHttpRequest>()
        verify {
            braintreeHttpClient.sendRequestSync(
                capture(httpRequestSlot),
                configuration,
                authorization,
            )
        }

        assertEquals(HttpMethod.POST, httpRequestSlot.captured.method)
        assertEquals("sample-url", httpRequestSlot.captured.path)
        assertEquals("{}", httpRequestSlot.captured.data)

        verify { httpResponseCallback.onResult(any(), any()) }
    }

    @Test
    fun sendPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendPOST("sample-url", "{}", emptyMap(), httpResponseCallback)
        verify { httpResponseCallback.onResult(null, exception) }
    }

    @Test
    fun `sendPOST defaults additionalHeaders to an empty map`() {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(mockk<Configuration>(relaxed = true))
            .build()
        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)

        sut.sendPOST(
            url = "sample-url",
            data = "{}",
            responseCallback = mockk(relaxed = true)
        )
        threadScheduler.flushBackgroundThread()

        val httpRequestSlot = slot<InternalHttpRequest>()
        verify {
            braintreeHttpClient.sendRequestSync(capture(httpRequestSlot), any(), authorization)
        }

        assertEquals(emptyMap<String, String>(), httpRequestSlot.captured.additionalHeaders)
    }

    @Test
    fun `sendPOST sends additionalHeaders to httpClient post`() {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(mockk<Configuration>(relaxed = true))
            .build()
        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        val headers = mapOf("name" to "value")

        sut.sendPOST(
            url = "sample-url",
            data = "{}",
            additionalHeaders = headers,
            responseCallback = mockk(relaxed = true)
        )
        threadScheduler.flushBackgroundThread()

        val httpRequestSlot = slot<InternalHttpRequest>()
        verify {
            braintreeHttpClient.sendRequestSync(capture(httpRequestSlot), any(), authorization)
        }
        assertEquals(headers, httpRequestSlot.captured.additionalHeaders)
    }

    @Test
    fun sendPOST_whenInvalidAuth_callsBackAuthError() {
        val sut = BraintreeClient(context, "invalid-auth-string")

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendPOST("sample-url", "{}", emptyMap(), httpResponseCallback)

        val authErrorSlot = slot<BraintreeException>()
        verify { httpResponseCallback.onResult(isNull(), capture(authErrorSlot)) }

        assertEquals(expectedAuthException.message, authErrorSlot.captured.message)
    }

    @Test
    fun sendGraphQLPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendGraphQLPOST(JSONObject(), httpResponseCallback)
        threadScheduler.flushBackgroundThread()
        threadScheduler.flushMainThread()

        verify {
            braintreeGraphQLClient.post("", "{}", configuration, authorization)
        }

        verify { httpResponseCallback.onResult(any(), any()) }
    }

    @Test
    fun sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendGraphQLPOST(JSONObject(), httpResponseCallback)
        verify { httpResponseCallback.onResult(null, exception) }
    }

    @Test
    fun sendGraphQLPOST_whenInvalidAuth_callsBackAuthError() {
        val sut = BraintreeClient(context, "invalid-auth-string")

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendGraphQLPOST(JSONObject(), httpResponseCallback)

        val authErrorSlot = slot<BraintreeException>()
        verify { httpResponseCallback.onResult(isNull(), capture(authErrorSlot)) }

        assertEquals(expectedAuthException.message, authErrorSlot.captured.message)
    }

    @Test
    @Throws(JSONException::class)
    fun sendAnalyticsEvent_sendsEventToAnalyticsClient() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        sut.sendAnalyticsEvent("event.started")

        verify {
            analyticsClient.sendEvent(
                configuration,
                match { it.name == "event.started" },
                "session-id",
                IntegrationType.CUSTOM,
                authorization
            )
        }
    }

    @Test
    fun sendAnalyticsEvent_whenConfigurationLoadFails_doesNothing() {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(Exception("error"))
            .build()

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        sut.sendAnalyticsEvent("event.started")

        verify { analyticsClient wasNot Called }
    }

    @Test
    fun isUrlSchemeDeclaredInAndroidManifest_forwardsInvocationToManifestValidator() {
        every {
            manifestValidator.isUrlSchemeDeclaredInAndroidManifest(
                applicationContext,
                "a-url-scheme",
                FragmentActivity::class.java
            )
        } returns true

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        assertTrue(
            sut.isUrlSchemeDeclaredInAndroidManifest("a-url-scheme", FragmentActivity::class.java)
        )
    }

    @Test
    fun manifestActivityInfo_forwardsInvocationToManifestValidator() {
        val activityInfo = ActivityInfo()
        every {
            manifestValidator.getActivityInfo(applicationContext, FragmentActivity::class.java)
        } returns activityInfo

        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        assertSame(activityInfo, sut.getManifestActivityInfo(FragmentActivity::class.java))
    }

    @Test
    fun returnUrlScheme_returnsUrlSchemeBasedOnApplicationIdByDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sut = BraintreeClient(BraintreeOptions(context, authorization))
        assertEquals("com.braintreepayments.api.core.test.braintree", sut.getReturnUrlScheme())
    }

    @Test
    fun returnUrlScheme_returnsUrlSchemeDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val returnUrlScheme = "custom-url-scheme"
        val sut = BraintreeClient(
            BraintreeOptions(
                context, authorization, returnUrlScheme =
                returnUrlScheme
            )
        )
        assertEquals("custom-url-scheme", sut.getReturnUrlScheme())
    }

    @Test
    fun returnUrlScheme_whenDefaultDeepLinkHandlerEnabled_returnsDefaultDeepLinkHandlerScheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sut = BraintreeClient(BraintreeOptions(context, authorization))
        sut.launchesBrowserSwitchAsNewTask(true)
        assertEquals(
            "com.braintreepayments.api.core.test.braintree.deeplinkhandler",
            sut.getReturnUrlScheme()
        )
    }

    @Test
    fun sessionId_returnsSessionIdDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionId = "custom-session-id"
        val sut = BraintreeClient(context, authorization, sessionId, IntegrationType.DROP_IN)
        assertEquals("custom-session-id", sut.sessionId)
    }

    @Test
    fun integrationType_returnsCustomByDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sut = BraintreeClient(BraintreeOptions(context, authorization))
        assertEquals("custom", sut.integrationType.stringValue)
    }

    @Test
    fun integrationType_returnsIntegrationTypeDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionId = "custom-session-id"
        val sut = BraintreeClient(context, authorization, sessionId, IntegrationType.DROP_IN)
        assertEquals("dropin", sut.integrationType.stringValue)
    }

    @Test
    @Throws(JSONException::class)
    fun reportCrash_reportsCrashViaAnalyticsClient() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()
        val params = createDefaultParams(configurationLoader)
        val sut = BraintreeClient(params)
        sut.reportCrash()

        val callbackSlot = slot<ConfigurationLoaderCallback>()
        verify {
            configurationLoader.loadConfiguration(authorization, capture(callbackSlot))
        }

        callbackSlot.captured.onResult(ConfigurationLoaderResponse(configuration))

        verify {
            analyticsClient.reportCrash(
                applicationContext,
                any(),
                "session-id",
                IntegrationType.CUSTOM,
                authorization
            )
        }
    }

    private fun createDefaultParams(
        configurationLoader: ConfigurationLoader
    ): BraintreeClientParams =
        BraintreeClientParams(
            context = context,
            sessionId = "session-id",
            authorization = authorization,
            returnUrlScheme = "sample-return-url-scheme",
            appLinkReturnUri = Uri.parse("https://example.com"),
            httpClient = braintreeHttpClient,
            graphQLClient = braintreeGraphQLClient,
            analyticsClient = analyticsClient,
            browserSwitchClient = browserSwitchClient,
            manifestValidator = manifestValidator,
            configurationLoader = configurationLoader,
            integrationType = IntegrationType.CUSTOM,
            threadScheduler = threadScheduler
        )
}
