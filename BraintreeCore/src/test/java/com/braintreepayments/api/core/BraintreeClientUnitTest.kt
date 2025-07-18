package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import com.braintreepayments.api.sharedutils.ManifestValidator
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.testutils.Fixtures
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
    private lateinit var merchantRepository: MerchantRepository

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

        val clientSDKSetupURL =
            "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
        val message = "Valid authorization required. See $clientSDKSetupURL for more info."
        expectedAuthException = BraintreeException(message)

        every { context.applicationContext } returns applicationContext
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    @Throws(JSONException::class)
    fun configuration_onAuthorizationAndConfigurationLoadSuccess_forwardsResult() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()

        val sut = createBraintreeClient(configurationLoader)
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

        val sut = createBraintreeClient(configurationLoader)

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

        val sut = createBraintreeClient(configurationLoader)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        val networkResponseCallbackSlot = slot<NetworkResponseCallback>()

        sut.sendGET("sample-url", httpResponseCallback)
        verify {
            braintreeHttpClient.get(
                "sample-url",
                configuration,
                authorization,
                capture(networkResponseCallbackSlot)
            )
        }

        assertTrue(networkResponseCallbackSlot.isCaptured)
    }

    @Test
    fun sendGET_onGetConfigurationFailure_forwardsErrorToCallback() {
        val configError = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(configError)
            .build()

        val sut = createBraintreeClient(configurationLoader)

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

        val sut = createBraintreeClient(configurationLoader)

        val networkResponseCallbackSlot = slot<NetworkResponseCallback>()
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        sut.sendPOST("sample-url", "{}", emptyMap(), httpResponseCallback)

        verify {
            braintreeHttpClient.post(
                path = "sample-url",
                data = "{}",
                configuration = configuration,
                authorization = authorization,
                callback = capture(networkResponseCallbackSlot)
            )
        }

        assertTrue(networkResponseCallbackSlot.isCaptured)
    }

    @Test
    fun sendPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val sut = createBraintreeClient(configurationLoader)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)

        sut.sendPOST("sample-url", "{}", emptyMap(), httpResponseCallback)
        verify { httpResponseCallback.onResult(null, exception) }
    }

    @Test
    fun `sendPOST defaults additionalHeaders to an empty map`() {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(mockk<Configuration>(relaxed = true))
            .build()
        val sut = createBraintreeClient(configurationLoader)

        sut.sendPOST(
            url = "sample-url",
            data = "{}",
            responseCallback = mockk(relaxed = true)
        )

        verify {
            braintreeHttpClient.post(
                path = any(),
                data = any(),
                configuration = any(),
                authorization = any(),
                additionalHeaders = emptyMap(),
                callback = any()
            )
        }
    }

    @Test
    fun `sendPOST sends additionalHeaders to httpClient post`() {
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(mockk<Configuration>(relaxed = true))
            .build()
        val sut = createBraintreeClient(configurationLoader)
        val headers = mapOf("name" to "value")

        sut.sendPOST(
            url = "sample-url",
            data = "{}",
            additionalHeaders = headers,
            responseCallback = mockk(relaxed = true)
        )

        verify {
            braintreeHttpClient.post(
                path = any(),
                data = any(),
                configuration = any(),
                authorization = any(),
                additionalHeaders = headers,
                callback = any()
            )
        }
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

        val sut = createBraintreeClient(configurationLoader)
        val httpResponseCallback = mockk<HttpResponseCallback>(relaxed = true)
        val networkResponseCallbackSlot = slot<NetworkResponseCallback>()

        sut.sendGraphQLPOST(JSONObject(), httpResponseCallback)
        verify {
            braintreeGraphQLClient.post(
                "{}",
                configuration,
                authorization,
                capture(networkResponseCallbackSlot)
            )
        }

        assertTrue(networkResponseCallbackSlot.isCaptured)
    }

    @Test
    fun sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        val exception = Exception("configuration error")
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configurationError(exception)
            .build()

        val sut = createBraintreeClient(configurationLoader)
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
    fun reportCrash_reportsCrashViaAnalyticsClient() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        val configurationLoader = MockkConfigurationLoaderBuilder()
            .configuration(configuration)
            .build()
        val sut = createBraintreeClient(configurationLoader)
        sut.reportCrash()

        val callbackSlots = mutableListOf<ConfigurationLoaderCallback>()
        verify {
            configurationLoader.loadConfiguration(capture(callbackSlots))
        }

        callbackSlots[0].onResult(ConfigurationLoaderResult.Success(configuration))

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
        merchantRepository: MerchantRepository = MerchantRepository.instance
    ) = BraintreeClient(
        applicationContext = applicationContext,
        integrationType = IntegrationType.CUSTOM,
        authorization = authorization,
        returnUrlScheme = "sample-return-url-scheme",
        appLinkReturnUri = appLinkReturnUri,
        httpClient = braintreeHttpClient,
        graphQLClient = braintreeGraphQLClient,
        analyticsClient = analyticsClient,
        manifestValidator = manifestValidator,
        configurationLoader = configurationLoader,
        merchantRepository = merchantRepository,
    )
}
