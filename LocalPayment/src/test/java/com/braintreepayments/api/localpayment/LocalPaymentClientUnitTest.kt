package com.braintreepayments.api.localpayment

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.core.ConfigurationException
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.localpayment.LocalPaymentNonce.Companion.fromJSON
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
class LocalPaymentClientUnitTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activity: FragmentActivity
    private lateinit var localPaymentAuthCallback: LocalPaymentAuthCallback
    private lateinit var localPaymentTokenizeCallback: LocalPaymentTokenizeCallback
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var dataCollector: DataCollector
    private lateinit var localPaymentApi: LocalPaymentApi
    private lateinit var localPaymentAuthRequestParams: LocalPaymentAuthRequestParams
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var payPalEnabledConfig: Configuration
    private lateinit var payPalDisabledConfig: Configuration
    private lateinit var sut: LocalPaymentClient

    private fun createLocalPaymentRequest(): LocalPaymentRequest {
        val address = PostalAddress().apply {
            streetAddress = "836486 of 22321 Park Lake"
            extendedAddress = "Apt 2"
            countryCodeAlpha2 = "NL"
            locality = "Den Haag"
            region = "CA"
            postalCode = "2585 GJ"
        }
        return LocalPaymentRequest(
            hasUserLocationConsent = true,
            address = address,
            amount = "1.10",
            bankIdentificationCode = "bank-id-code",
            currencyCode = "EUR",
            displayName = "My Brand!",
            email = "jon@getbraintree.com",
            givenName = "Jon",
            merchantAccountId = "local-merchant-account-id",
            paymentType = "ideal",
            paymentTypeCountryCode = "NL",
            phone = "639847934",
            isShippingAddressRequired = true,
            surname = "Doe"
        )
    }

    private fun createLocalPaymentAuthRequestParams(
        request: LocalPaymentRequest = createLocalPaymentRequest(),
        approvalUrl: String = "https://sample.com/approval?token=sample-token",
        paymentId: String = "payment-id"
    ): LocalPaymentAuthRequestParams {
        return LocalPaymentAuthRequestParams(request, approvalUrl, paymentId)
    }
    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        activity = mockk<FragmentActivity>(relaxed = true)
        localPaymentAuthCallback = mockk<LocalPaymentAuthCallback>(relaxed = true)
        localPaymentTokenizeCallback = mockk<LocalPaymentTokenizeCallback>(relaxed = true)

        braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
            .build()
        dataCollector = mockk<DataCollector>(relaxed = true)
        localPaymentApi = mockk<LocalPaymentApi>(relaxed = true)
        analyticsParamRepository = mockk<AnalyticsParamRepository>(relaxed = true)
        localPaymentAuthRequestParams = mockk<LocalPaymentAuthRequestParams>(relaxed = true)
        val testScope = TestScope(testDispatcher)
        sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        every { localPaymentAuthRequestParams.approvalUrl } returns "https://"
        every { localPaymentAuthRequestParams.request } returns createLocalPaymentRequest()
        every { localPaymentAuthRequestParams.paymentId } returns "paymentId"
        every { analyticsParamRepository.sessionId } returns "sample-session-id"

        payPalEnabledConfig = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        payPalDisabledConfig = fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL)
    }

    @Test
    fun `when createPaymentAuthRequest is called, resets the analytics session id`() = runTest(testDispatcher) {
        sut.createPaymentAuthRequest(createLocalPaymentRequest(), localPaymentAuthCallback)
        advanceUntilIdle()

        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun `when createPaymentAuthRequest is called, sends a payment started analytics event`() = runTest(testDispatcher) {
        sut.createPaymentAuthRequest(createLocalPaymentRequest(), localPaymentAuthCallback)
        advanceUntilIdle()

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_STARTED,
                AnalyticsEventParams(),
                true
            )
        }
    }

    @Test
    fun `when paymentType is null, createPaymentAuthRequest sends a payment failed analytics event`() = runTest(testDispatcher) {
        val request = createLocalPaymentRequest()
        request.paymentType = null
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        val errorDescription =
            "LocalPaymentRequest is invalid, paymentType and amount are required."
        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_FAILED,
                AnalyticsEventParams(errorDescription = errorDescription),
                true
            )
        }
    }

    @Test
    fun `when createPaymentAuthRequest is called, creates payment method via localPaymentApi`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val localPaymentApi = mockk<LocalPaymentApi>(relaxed = true)

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val request = createLocalPaymentRequest()
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        coVerify {
            localPaymentApi.createPaymentMethod(request)
        }
    }

    @Test
    fun `when createPaymentMethod succeeds, createPaymentAuthRequest forwards result params to callback`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()

        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val request = createLocalPaymentRequest()
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        assertEquals(localPaymentAuthRequestParams, params)
    }

    @Test
    fun `when createPaymentMethod succeeds, createPaymentAuthRequest sends a browser switch succeeded analytics event`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val request = createLocalPaymentRequest()
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
                AnalyticsEventParams("paymentId"),
                true
            )
        }
    }

    @Test
    fun `when configuration fetch fails, createPaymentAuthRequest forwards the error to callback`() = runTest(testDispatcher) {
        val configException = IOException("Configuration not fetched")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configException)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val request = createLocalPaymentRequest()
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertEquals(configException, exception)
    }

    @Test
    fun `when localPaymentApi createPaymentMethod fails, createPaymentAuthRequest sends a payment failed analytics event`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()

        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .createPaymentMethodError(Exception("error"))
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val request = createLocalPaymentRequest()
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        val errorDescription = "An error occurred creating the local payment method: error"
        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_FAILED,
                AnalyticsEventParams(errorDescription = errorDescription),
                true
            )
        }
    }

    @Test
    fun `when PayPal is disabled in configuration, createPaymentAuthRequest returns a ConfigurationException to callback`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalDisabledConfig)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val request = createLocalPaymentRequest()
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assert(exception is ConfigurationException)
        assertEquals("Local payments are not enabled for this merchant.", exception.message)
    }

    @Test
    fun `when request amount is null, createPaymentAuthRequest returns a validation error to callback`() = runTest(testDispatcher) {
        val request = createLocalPaymentRequest()
        request.amount = null

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assert(exception is BraintreeException)
        assertEquals(
            "LocalPaymentRequest is invalid, paymentType and amount are required.",
            exception.message
        )
    }

    @Test
    fun `when request paymentType is null, createPaymentAuthRequest returns a validation error to callback`() = runTest(testDispatcher) {
        val request = createLocalPaymentRequest()
        request.paymentType = null

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assert(exception is BraintreeException)
        assertEquals(
            "LocalPaymentRequest is invalid, paymentType and amount are required.",
            exception.message
        )
    }

    @Test
    fun `when createPaymentMethod fails, createPaymentAuthRequest returns a wrapped BraintreeException to callback`() = runTest(testDispatcher) {
        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .createPaymentMethodError(Exception("error"))
            .build()
        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )

        sut.createPaymentAuthRequest(
            createLocalPaymentRequest(),
            localPaymentAuthCallback
        )
        advanceUntilIdle()

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assert(exception is BraintreeException)
        assertEquals(
            "An error occurred creating the local payment method: error",
            exception.message
        )
    }

    @Test
    fun `when createPaymentMethod succeeds, createPaymentAuthRequest returns ReadyToLaunch params to callback`() =
        runTest(testDispatcher) {
        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )

        sut.createPaymentAuthRequest(createLocalPaymentRequest(), localPaymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        assertEquals(localPaymentAuthRequestParams, params)
    }

    @Test
    fun `when payment id is empty, createPaymentAuthRequest sends analytics events without a payment id`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val request = createLocalPaymentRequest()
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = createLocalPaymentAuthRequestParams(request, approvalUrl, "")

        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(transaction)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        sut.createPaymentAuthRequest(createLocalPaymentRequest(), localPaymentAuthCallback)
        advanceUntilIdle()

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_STARTED,
                AnalyticsEventParams(),
                true
            )
        }
        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
                AnalyticsEventParams(),
                true
            )
        }
    }

    @Test
    fun `when payment id is present, createPaymentAuthRequest sends analytics events with that payment id`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val request = createLocalPaymentRequest()
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = createLocalPaymentAuthRequestParams(request, approvalUrl, "some-paypal-context-id")

        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(transaction)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        sut.createPaymentAuthRequest(createLocalPaymentRequest(), localPaymentAuthCallback)
        advanceUntilIdle()

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_STARTED,
                AnalyticsEventParams(),
                true
            )
        }
        val params = AnalyticsEventParams("some-paypal-context-id")
        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
                params,
                true
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `when buildBrowserSwitchOptions is called, returns ReadyToLaunch params with expected browser switch options`() = runTest(testDispatcher) {
        val request = createLocalPaymentRequest()
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        val paymentAuthRequest = sut.buildBrowserSwitchOptions(transaction, true)

        assert(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        val browserSwitchOptions = params.browserSwitchOptions
        assertEquals(BraintreeRequestCodes.LOCAL_PAYMENT.code, browserSwitchOptions!!.getRequestCode())
        assertEquals(Uri.parse("https://sample.com/approval?token=sample-token"), browserSwitchOptions.getUrl())
        assertFalse(browserSwitchOptions.isLaunchAsNewTask())

        val metadata = browserSwitchOptions.getMetadata()
        val expectedMetadata = JSONObject()
            .put("merchant-account-id", "local-merchant-account-id")
            .put("payment-type", "ideal")
            .put("has-user-location-consent", true)

        JSONAssert.assertEquals(expectedMetadata, metadata, true)
    }

    @Test
    fun `when buildBrowserSwitchOptions is called, isLaunchAsNewTask defaults to false`() = runTest(testDispatcher) {
        every { braintreeClient.launchesBrowserSwitchAsNewTask() } returns true

        val request = createLocalPaymentRequest()
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = createLocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        val paymentAuthRequest = sut.buildBrowserSwitchOptions(transaction, true)

        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        val browserSwitchOptions = params.browserSwitchOptions

        assertFalse(browserSwitchOptions!!.isLaunchAsNewTask())
    }

    @Test
    fun `when buildBrowserSwitchOptions is called, sends a browser switch succeeded analytics event`() = runTest(testDispatcher) {
        val request = createLocalPaymentRequest()
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = createLocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        sut.buildBrowserSwitchOptions(transaction, true)

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
                AnalyticsEventParams(),
                true
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `when tokenize POST fails, notifies callback of the error and sends a payment failed analytics event`() = runTest(testDispatcher) {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        val postError = Exception("POST failed")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .sendPostErrorResponse(postError)
            .build()

        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .tokenizeError(postError)
            .build()

        every {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                false
            )
        } returns "sample-correlation-id"

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentResult>()
        verify { localPaymentTokenizeCallback.onLocalPaymentResult(capture(slot)) }

        val result = slot.captured
        assert(result is LocalPaymentResult.Failure)
        val exception = (result as LocalPaymentResult.Failure).error
        assertEquals(postError, exception)

        val params = AnalyticsEventParams(
            errorDescription = "POST failed"
        )
        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_FAILED,
                params,
                true
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `when browser switch result is success, tokenize calls localPaymentApi tokenize with expected params`() = runTest(testDispatcher) {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        // Ensure the correct config is used
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val localPaymentApi = mockk<LocalPaymentApi>(relaxed = true)

        every {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                false
            )
        } returns "sample-correlation-id"

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)
        advanceUntilIdle()

        coVerify {
            localPaymentApi.tokenize(
                eq("local-merchant-account-id"),
                eq(webUrl),
                eq("sample-correlation-id")
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `when tokenization succeeds, tokenize sends the nonce result to callback`() = runTest(testDispatcher) {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        every {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                false
            )
        } returns "client-metadata-id"

        val successNonce = fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
        )
        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .tokenizeSuccess(successNonce)
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentResult>()
        verify { localPaymentTokenizeCallback.onLocalPaymentResult(capture(slot)) }

        val result = slot.captured
        assert(result is LocalPaymentResult.Success)
        val nonce = (result as LocalPaymentResult.Success).nonce
        assertEquals(successNonce, nonce)
    }

    @Test
    @Throws(JSONException::class)
    fun `when tokenization succeeds, tokenize sends a payment succeeded analytics event`() = runTest(testDispatcher) {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        every {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                false
            )
        } returns "client-metadata-id"

        val localPaymentApi = MockkLocalPaymentApiBuilder()
            .tokenizeSuccess(
                fromJSON(
                    JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
                )
            )
            .build()

        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)
        advanceUntilIdle()

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_SUCCEEDED,
                AnalyticsEventParams(),
                true
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `when configuration fetch fails during tokenize, returns the configuration error to callback`() = runTest(testDispatcher) {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        val configError = IOException("config error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val localPaymentApi = mockk<LocalPaymentApi>(relaxed = true) // Add this line

        every {
            dataCollector.getClientMetadataId(
                any<FragmentActivity>(),
                any<Configuration>(),
                any<Boolean>()
            )
        } returns "sample-correlation-id"

        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)
        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentResult>()
        verify { localPaymentTokenizeCallback.onLocalPaymentResult(capture(slot)) }

        val result = slot.captured
        assert(result is LocalPaymentResult.Failure)
        val exception = (result as LocalPaymentResult.Failure).error
        assertEquals(configError, exception)
    }

    @Test
    @Throws(JSONException::class)
    fun `when the return url indicates a user cancellation, tokenize notifies callback and sends a payment canceled analytics event`() = runTest(testDispatcher) {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)
        sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository,
            testDispatcher, this
        )

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)
        advanceUntilIdle()

        val slot = slot<LocalPaymentResult>()
        verify { localPaymentTokenizeCallback.onLocalPaymentResult(capture(slot)) }

        val result = slot.captured
        assert(result is LocalPaymentResult.Cancel)
        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_CANCELED,
                AnalyticsEventParams(),
                true
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `when request has-user-location-consent metadata is true, tokenize passes that value to getClientMetadataId`() =
    runTest(testDispatcher) {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
            .put("has-user-location-consent", true)
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()

        every {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                any()
            )
        } returns "client-metadata-id"

        val request = createLocalPaymentRequest()
        sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository,
            testDispatcher,
            this
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        advanceUntilIdle()
        sut.tokenize(
            activity,
            localPaymentAuthResult,
            mockk(relaxed = true)
        )
        advanceUntilIdle()

        verify {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                eq(true)
            )
        }
    }

    @Test
    fun `when braintreeClient throws CancellationException, createPaymentAuthRequest never invokes the callback`() =
    runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(kotlin.coroutines.cancellation.CancellationException("cancelled"))
            .build()

        val testScope = TestScope(testDispatcher)
        sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )
        sut.createPaymentAuthRequest(createLocalPaymentRequest(), localPaymentAuthCallback)
        advanceUntilIdle()

        verify(exactly = 0) { localPaymentAuthCallback.onLocalPaymentAuthRequest(any()) }
    }

    @Test
    fun `when braintreeClient throws CancellationException, tokenize never invokes the callback`() =
    runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(kotlin.coroutines.cancellation.CancellationException("cancelled"))
            .build()

        val testScope = TestScope(testDispatcher)
        sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository,
            testDispatcher,
            testScope
        )

        val successUrl = Uri.parse("https://example.com/local-payment-success?token=token")
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchFinalResult.returnUrl } returns successUrl
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchFinalResult)
        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)
        advanceUntilIdle()

        verify(exactly = 0) { localPaymentTokenizeCallback.onLocalPaymentResult(any()) }
    }
}
