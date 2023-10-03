package com.braintreepayments.api

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BraintreeClientUnitTest {

    private lateinit var authorization: Authorization
    private lateinit var context: Context
    private lateinit var applicationContext: Context
    private lateinit var braintreeHttpClient: BraintreeHttpClient
    private lateinit var braintreeGraphQLClient: BraintreeGraphQLClient
    private lateinit var configurationLoader: ConfigurationLoader
    private lateinit var authorizationLoader: AuthorizationLoader
    private lateinit var analyticsClient: AnalyticsClient
    private lateinit var manifestValidator: ManifestValidator
    private lateinit var browserSwitchClient: BrowserSwitchClient

    @Before
    fun beforeEach() {
        authorization = mockk(relaxed = true)
        context = mockk(relaxed = true)
        applicationContext = ApplicationProvider.getApplicationContext()

        braintreeHttpClient = mockk(relaxed = true)
        braintreeGraphQLClient = mockk(relaxed = true)
        configurationLoader = mockk(relaxed = true)
        authorizationLoader = mockk(relaxed = true)

        analyticsClient = mockk(relaxed = true)
        manifestValidator = mockk(relaxed = true)
        browserSwitchClient = mockk(relaxed = true)

        every { context.applicationContext } returns applicationContext
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun constructor_usesSessionIdFromParams() {
        val params = BraintreeOptions(context = context, sessionId = "session-id")
        val sut = BraintreeClient(params)
        assertEquals("session-id", sut.sessionId)
    }

    @Test
    fun constructor_setsSessionIdFromUUIDHelperIfSessionIdNotIncluded() {
        val uuidRegex = """[a-fA-F0-9]{32}""".toRegex()

        val sut = BraintreeClient(BraintreeOptions(context = context))
        assertTrue(uuidRegex.matches(sut.sessionId))
    }

    @Test
    @Throws(JSONException::class)
    fun configuration_onAuthorizationAndConfigurationLoadSuccess_forwardsResult() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)

        verify { callback.onResult(configuration, null) }
    }

    @Test
    fun configuration_forwardsAuthorizationLoaderError() {
        val authFetchError = Exception("auth fetch error")
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorizationError(authFetchError)
            .build()
        val callback = mockk<ConfigurationCallback>(relaxed = true)

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        sut.getConfiguration(callback)
        verify { callback.onResult(null, authFetchError) }
    }

    @Test
    fun configuration_forwardsConfigurationLoaderError() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val configFetchError = Exception("config fetch error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configFetchError)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        val callback = mockk<ConfigurationCallback>(relaxed = true)
        sut.getConfiguration(callback)

        verify { callback.onResult(null, configFetchError) }
    }

    @Test
    fun authorization_forwardsInvocationToAuthorizationLoader() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder().build()
        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        val callback = mockk<AuthorizationCallback>(relaxed = true)
        sut.getAuthorization(callback)

        verify { authorizationLoader.loadAuthorization(any()) }
    }

    @Test
    fun invalidateClientToken_forwardsInvocationToAuthorizationLoader() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder().build()
        val params = createDefaultParams(configurationLoader, authorizationLoader)

        val sut = BraintreeClient(params)
        sut.invalidateClientToken()

        verify { authorizationLoader.invalidateClientToken() }
    }

    @Test
    fun sendGET_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendGET("sample-url", httpResponseCallback)
        verify {
            braintreeHttpClient.get(
                "sample-url",
                configuration,
                authorization,
                httpResponseCallback
            )
        }
    }

    @Test
    fun sendGET_onGetAuthorizationFailure_forwardsErrorToCallback() {
        val authorizationError = Exception("authorization error")
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorizationError(authorizationError)
            .build()

        val configurationLoader = MockkConfigurationLoaderBuilder().build()
        val params = createDefaultParams(configurationLoader, authorizationLoader)

        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendGET("sample-url", httpResponseCallback)
        verify { httpResponseCallback.onResult(null, authorizationError) }
    }

    @Test
    fun sendGET_onGetConfigurationFailure_forwardsErrorToCallback() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val configError = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configError)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendGET("sample-url", httpResponseCallback)

        verify { httpResponseCallback.onResult(null, configError) }
    }

    @Test
    fun sendPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendPOST("sample-url", "{}", httpResponseCallback)

        verify {
            braintreeHttpClient.post(
                "sample-url",
                "{}",
                configuration,
                authorization,
                httpResponseCallback
            )
        }
    }

    @Test
    fun sendPOST_onAuthorizationFailure_forwardsErrorToCallback() {
        val authError = Exception("authorization error")
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorizationError(authError)
            .build()

        val configurationLoader = MockkConfigurationLoaderBuilder().build()
        val params = createDefaultParams(configurationLoader, authorizationLoader)

        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendPOST("sample-url", "{}", httpResponseCallback)

        verify { httpResponseCallback.onResult(null, authError) }
    }

    @Test
    fun sendPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendPOST("sample-url", "{}", httpResponseCallback)
        verify { httpResponseCallback.onResult(null, exception) }
    }

    @Test
    fun sendGraphQLPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val configuration = mockk<Configuration>(relaxed = true)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendGraphQLPOST("{}", httpResponseCallback)
        verify {
            braintreeGraphQLClient.post(
                "{}",
                configuration,
                authorization,
                httpResponseCallback
            )
        }
    }

    @Test
    fun sendGraphQLPOST_onAuthorizationFailure_forwardsErrorToCallback() {
        val authError = Exception("authorization error")
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorizationError(authError)
            .build()

        val configurationLoader = MockkConfigurationLoaderBuilder().build()
        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendGraphQLPOST("{}", httpResponseCallback)

        verify { httpResponseCallback.onResult(null, authError) }
    }

    @Test
    fun sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendGraphQLPOST("{}", httpResponseCallback)
        verify { httpResponseCallback.onResult(null, exception) }
    }

    @Test
    @Throws(JSONException::class)
    fun sendAnalyticsEvent_sendsEventToAnalyticsClient() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()

        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        sut.sendAnalyticsEvent("event.started")

        verify {
            analyticsClient.sendEvent(
                configuration, "event.started", "session-id", "custom", authorization
            )
        }
    }

    @Test
    fun sendAnalyticsEvent_whenAuthorizationLoadFails_doesNothing() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorizationError(Exception("error"))
            .build()
        val configurationLoader = MockkConfigurationLoaderBuilder().build()
        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        sut.sendAnalyticsEvent("event.started")
        verify { analyticsClient wasNot Called }
    }

    @Test
    fun sendAnalyticsEvent_whenConfigurationLoadFails_doesNothing() {
        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(Exception("error"))
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        sut.sendAnalyticsEvent("event.started")

        verify { analyticsClient wasNot Called }
    }

    @Test
    @Throws(JSONException::class)
    fun sendAnalyticsEvent_whenAnalyticsNotEnabled_doesNothing() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ANALYTICS)

        val authorizationLoader = MockkAuthorizationLoaderBuilder()
            .authorization(authorization)
            .build()
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        sut.sendAnalyticsEvent("event.started")

        verify { analyticsClient wasNot Called }
    }

    @Test
    @Throws(BrowserSwitchException::class)
    fun startBrowserSwitch_forwardsInvocationToBrowserSwitchClient() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        val browserSwitchOptions = BrowserSwitchOptions()

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)

        sut.startBrowserSwitch(activity, browserSwitchOptions)
        verify { browserSwitchClient.start(activity, browserSwitchOptions) }
    }

    @Test
    fun browserSwitchResult_forwardsInvocationToBrowserSwitchClient() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        val browserSwitchResult = createSuccessfulBrowserSwitchResult()
        every { browserSwitchClient.getResult(activity) } returns browserSwitchResult

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        assertSame(browserSwitchResult, sut.getBrowserSwitchResult(activity))
    }

    @Test
    fun deliverBrowserSwitchResult_forwardsInvocationToBrowserSwitchClient() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        val params = createDefaultParams(configurationLoader, authorizationLoader)

        val sut = BraintreeClient(params)
        sut.deliverBrowserSwitchResult(activity)

        verify { browserSwitchClient.deliverResult(activity) }
    }

    @Test
    fun deliverBrowserSwitchResultFromNewTask_forwardsInvocationToBrowserSwitchClient() {
        val context = mockk<Context>(relaxed = true)
        val params = createDefaultParams(configurationLoader, authorizationLoader)

        val sut = BraintreeClient(params)
        sut.deliverBrowserSwitchResultFromNewTask(context)

        verify { browserSwitchClient.deliverResultFromCache(context) }
    }

    @Test
    fun parseBrowserSwitchResult_forwardsInvocationToBrowserSwitchClient() {
        val context = mockk<Context>(relaxed = true)
        val params = createDefaultParams(configurationLoader, authorizationLoader)

        val expected = mock<BrowserSwitchResult>()
        val intent = Intent()
        every { browserSwitchClient.parseResult(context, 123, intent) } returns expected

        val sut = BraintreeClient(params)
        val actual = sut.parseBrowserSwitchResult(context, 123, intent)
        assertSame(expected, actual)
    }

    @Test
    fun clearActiveBrowserSwitchRequests_forwardsInvocationToBrowserSwitchClient() {
        val context = mockk<Context>(relaxed = true)
        val params = createDefaultParams(configurationLoader, authorizationLoader)

        val sut = BraintreeClient(params)
        sut.clearActiveBrowserSwitchRequests(context)

        verify { browserSwitchClient.clearActiveRequests(context) }
    }

    @Test
    @Throws(BrowserSwitchException::class)
    fun assertCanPerformBrowserSwitch_assertsBrowserSwitchIsPossible() {
        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val activity = mockk<FragmentActivity>(relaxed = true)

        val sut = BraintreeClient(params)
        sut.assertCanPerformBrowserSwitch(activity, 123)

        val browserSwitchOptionsSlot = slot<BrowserSwitchOptions>()
        verify {
            browserSwitchClient.assertCanPerformBrowserSwitch(
                activity,
                capture(browserSwitchOptionsSlot)
            )
        }

        val browserSwitchOptions = browserSwitchOptionsSlot.captured
        assertEquals(123, browserSwitchOptions.requestCode.toLong())
        assertEquals(Uri.parse("https://braintreepayments.com"), browserSwitchOptions.url)
    }

    @Test
    @Throws(BrowserSwitchException::class)
    @Suppress("SwallowedException")
    fun assertCanPerformBrowserSwitch_onSuccess_doesNotThrow() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        every { browserSwitchClient.assertCanPerformBrowserSwitch(activity, any()) } returns Unit

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        try {
            sut.assertCanPerformBrowserSwitch(activity, 123)
        } catch (e: BrowserSwitchException) {
            fail("shouldn't get here")
        }
    }

    @Test
    @Throws(BrowserSwitchException::class)
    fun assertCanPerformBrowserSwitch_onError_throws() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        val browserSwitchException = BrowserSwitchException("error")

        every {
            browserSwitchClient.assertCanPerformBrowserSwitch(activity, any())
        } throws browserSwitchException

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        try {
            sut.assertCanPerformBrowserSwitch(activity, 123)
            fail("shouldn't get here")
        } catch (e: BrowserSwitchException) {
            assertSame(browserSwitchException, e)
        }
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

        val params = createDefaultParams(configurationLoader, authorizationLoader)
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

        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        assertSame(activityInfo, sut.getManifestActivityInfo(FragmentActivity::class.java))
    }

    @Test
    fun returnUrlScheme_returnsUrlSchemeBasedOnApplicationIdByDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authorization = Fixtures.BASE64_CLIENT_TOKEN
        val sut = BraintreeClient(context, authorization)
        assertEquals("com.braintreepayments.api.test.braintree", sut.getReturnUrlScheme())
    }

    @Test
    fun returnUrlScheme_returnsUrlSchemeDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authorization = Fixtures.BASE64_CLIENT_TOKEN
        val returnUrlScheme = "custom-url-scheme"
        val sut = BraintreeClient(context, authorization, returnUrlScheme)
        assertEquals("custom-url-scheme", sut.getReturnUrlScheme())
    }

    @Test
    fun returnUrlScheme_whenDefaultDeepLinkHandlerEnabled_returnsDefaultDeepLinkHandlerScheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authorization = Fixtures.BASE64_CLIENT_TOKEN
        val sut = BraintreeClient(context, authorization)
        sut.launchesBrowserSwitchAsNewTask(true)
        assertEquals(
            "com.braintreepayments.api.test.braintree.deeplinkhandler",
            sut.getReturnUrlScheme()
        )
    }

    @Test
    fun sessionId_withAuthString_returnsSessionIdDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authorization = Fixtures.BASE64_CLIENT_TOKEN
        val sessionId = "custom-session-id"
        val sut = BraintreeClient(context, authorization, sessionId, IntegrationType.DROP_IN)
        assertEquals("custom-session-id", sut.sessionId)
    }

    @Test
    fun sessionId_withClientTokenProvider_returnsSessionIdDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clientTokenProvider = mockk<ClientTokenProvider>(relaxed = true)
        val sessionId = "custom-session-id"
        val sut = BraintreeClient(context, clientTokenProvider, sessionId, IntegrationType.DROP_IN)
        assertEquals("custom-session-id", sut.sessionId)
    }

    @Test
    fun integrationType_returnsCustomByDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authorization = Fixtures.BASE64_CLIENT_TOKEN
        val sut = BraintreeClient(context, authorization)
        assertEquals("custom", sut.integrationType)
    }

    @Test
    fun integrationType_withAuthString_returnsIntegrationTypeDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authorization = Fixtures.BASE64_CLIENT_TOKEN
        val sessionId = "custom-session-id"
        val sut = BraintreeClient(context, authorization, sessionId, IntegrationType.DROP_IN)
        assertEquals("dropin", sut.integrationType)
    }

    @Test
    fun integrationType_withClientTokenProvider_returnsIntegrationTypeDefinedInConstructor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clientTokenProvider = mockk<ClientTokenProvider>(relaxed = true)
        val sessionId = "custom-session-id"
        val sut = BraintreeClient(context, clientTokenProvider, sessionId, IntegrationType.DROP_IN)
        assertEquals("dropin", sut.integrationType)
    }

    @Test
    @Throws(JSONException::class)
    fun reportCrash_reportsCrashViaAnalyticsClient() {
        every { authorizationLoader.authorizationFromCache } returns authorization

        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()
        val params = createDefaultParams(configurationLoader, authorizationLoader)
        val sut = BraintreeClient(params)
        sut.reportCrash()

        verify {
            analyticsClient.reportCrash(
                applicationContext,
                "session-id",
                IntegrationType.CUSTOM,
                authorization
            )
        }
    }

    private fun createDefaultParams(
        configurationLoader: ConfigurationLoader,
        authorizationLoader: AuthorizationLoader
    ): BraintreeClientParams =
        BraintreeClientParams(
            context = context,
            sessionId = "session-id",
            authorizationLoader = authorizationLoader,
            returnUrlScheme = "sample-return-url-scheme",
            httpClient = braintreeHttpClient,
            graphQLClient = braintreeGraphQLClient,
            analyticsClient = analyticsClient,
            browserSwitchClient = browserSwitchClient,
            manifestValidator = manifestValidator,
            configurationLoader = configurationLoader,
            integrationType = IntegrationType.CUSTOM
        )

    companion object {
        private fun createSuccessfulBrowserSwitchResult(): BrowserSwitchResult {
            val requestCode = 123
            val url = Uri.parse("www.example.com")
            val returnUrlScheme = "sample-scheme"
            val browserSwitchRequest = BrowserSwitchRequest(
                requestCode, url, JSONObject(), returnUrlScheme, true
            )
            return BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, browserSwitchRequest)
        }
    }
}
