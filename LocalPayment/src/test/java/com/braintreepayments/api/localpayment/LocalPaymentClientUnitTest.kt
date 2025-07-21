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
import io.mockk.*
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class LocalPaymentClientUnitTest {
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

        every { localPaymentAuthRequestParams!!.approvalUrl } returns "https://"
        every { localPaymentAuthRequestParams!!.request } returns this.idealLocalPaymentRequest
        every { localPaymentAuthRequestParams!!.paymentId } returns "paymentId"
        every { analyticsParamRepository!!.sessionId } returns "sample-session-id"

        payPalEnabledConfig = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        payPalDisabledConfig = fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL)
    }

    @Test
    fun createPaymentAuthRequest_resetsSessionId() {
        val sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository
        )
        sut.createPaymentAuthRequest(this.idealLocalPaymentRequest, localPaymentAuthCallback)

        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun createPaymentAuthRequest_sendsPaymentStartedEvent() {
        val sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository
        )
        sut.createPaymentAuthRequest(this.idealLocalPaymentRequest, localPaymentAuthCallback)

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_STARTED,
                AnalyticsEventParams(),
                true
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_sendsPaymentFailedEvent_forNullGetPaymentType() {
        val request = this.idealLocalPaymentRequest
        request.paymentType = null

        val sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository
        )
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

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
    fun createPaymentAuthRequest_createsPaymentMethodWithLocalPaymentApi() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val localPaymentApi = mockk<LocalPaymentApi>(relaxed = true)

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = this.idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

        verify {
            localPaymentApi.createPaymentMethod(
                request,
                any()
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_success_forwardsResultToCallback() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        assertEquals(localPaymentAuthRequestParams, params)
    }

    @Test
    fun createPaymentAuthRequest_success_sendsAnalyticsEvents() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
                AnalyticsEventParams("paymentId"),
                true
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_configurationFetchError_forwardsErrorToCallback() {
        val configException = Exception("Configuration not fetched")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(configException)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = this.idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertEquals(configException, exception)
    }

    @Test
    fun createPaymentAuthRequest_onLocalPaymentApiError_sendsAnalyticsEvents() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodError(Exception("error"))
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

        val errorDescription = "An error occurred creating the local payment method."
        verify {
            braintreeClient.sendAnalyticsEvent(
                LocalPaymentAnalytics.PAYMENT_FAILED,
                AnalyticsEventParams(errorDescription = errorDescription),
                true
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_whenPayPalDisabled_returnsErrorToCallback() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalDisabledConfig)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = this.idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assert(exception is ConfigurationException)
        assertEquals("Local payments are not enabled for this merchant.", exception.message)
    }

    @Test
    fun createPaymentAuthRequest_whenAmountIsNull_returnsErrorToCallback() {
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = this.idealLocalPaymentRequest
        request.amount = null

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

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
    fun createPaymentAuthRequest_whenPaymentTypeIsNull_returnsErrorToCallback() {
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val request = this.idealLocalPaymentRequest
        request.paymentType = null

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)

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
    fun createPaymentAuthRequest_whenCreatePaymentMethodError_returnsErrorToCallback() {
        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodError(Exception("error"))
            .build()
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )

        sut.createPaymentAuthRequest(
            this.idealLocalPaymentRequest,
            localPaymentAuthCallback
        )

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assert(exception is BraintreeException)
        assertEquals(
            "An error occurred creating the local payment method.",
            exception.message
        )
    }

    @Test
    fun createPaymentAuthRequest_whenCreatePaymentMethodSuccess_returnsLocalPaymentResultToCallback() {
        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )

        sut.createPaymentAuthRequest(this.idealLocalPaymentRequest, localPaymentAuthCallback)

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        assertEquals(localPaymentAuthRequestParams, params)
    }

    @Test
    fun createPaymentAuthRequest_success_withEmptyPaymentId_sendsAnalyticsEvents() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val request = this.idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "")

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(transaction)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        sut.createPaymentAuthRequest(this.idealLocalPaymentRequest, localPaymentAuthCallback)

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
    fun createPaymentAuthRequest_success_withPaymentId_sendsAnalyticsEvents() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .build()
        val request = this.idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "some-paypal-context-id")

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(transaction)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        sut.createPaymentAuthRequest(this.idealLocalPaymentRequest, localPaymentAuthCallback)

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
    fun buildBrowserSwitchOptions_returnsLocalPaymentResultWithBrowserSwitchOptions() {
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )

        val request = this.idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback)

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }

        val paymentAuthRequest = slot.captured
        assert(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        val browserSwitchOptions = params.browserSwitchOptions
        assertEquals(BraintreeRequestCodes.LOCAL_PAYMENT.code, browserSwitchOptions!!.getRequestCode())
        assertEquals(Uri.parse("https://sample.com/approval?token=sample-token"), browserSwitchOptions.getUrl())
        TestCase.assertFalse(browserSwitchOptions.isLaunchAsNewTask())

        val metadata = browserSwitchOptions.getMetadata()
        val expectedMetadata = JSONObject()
            .put("merchant-account-id", "local-merchant-account-id")
            .put("payment-type", "ideal")
            .put("has-user-location-consent", true)

        JSONAssert.assertEquals(expectedMetadata, metadata, true)
    }

    @Test
    fun testBrowserSwitchOptions_NewTask_RequestCode() {
        every { braintreeClient.launchesBrowserSwitchAsNewTask() } returns true
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )

        val request = idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback)

        val slot = slot<LocalPaymentAuthRequest>()
        verify { localPaymentAuthCallback.onLocalPaymentAuthRequest(capture(slot)) }
        val paymentAuthRequest = slot.captured
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        val browserSwitchOptions = params.browserSwitchOptions

        assertTrue(browserSwitchOptions!!.isLaunchAsNewTask())
    }

    @Test
    fun buildBrowserSwitchOptions_sendsAnalyticsEvents() {
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )

        val request = this.idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback)

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
    fun tokenize_whenPostFailure_notifiesCallbackOfErrorAlongWithAnalyticsEvent() {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        val postError = Exception("POST failed")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalEnabledConfig)
            .sendPOSTErrorResponse(postError)
            .build()

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .tokenizeError(postError)
            .build()

        every {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                false
            )
        } returns "sample-correlation-id"

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)

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
    fun tokenize_whenResultOKAndSuccessful_tokenizesWithLocalPaymentApi() {
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

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)

        verify {
            localPaymentApi.tokenize(
                eq("local-merchant-account-id"),
                eq(webUrl),
                eq("sample-correlation-id"),
                any()
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOKAndTokenizationSucceeds_sendsResultToCallback() {
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
        val localPaymentApi = MockLocalPaymentApiBuilder()
            .tokenizeSuccess(successNonce)
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)

        val slot = slot<LocalPaymentResult>()
        verify { localPaymentTokenizeCallback.onLocalPaymentResult(capture(slot)) }

        val result = slot.captured
        assert(result is LocalPaymentResult.Success)
        val nonce = (result as LocalPaymentResult.Success).nonce
        assertEquals(successNonce, nonce)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOKAndTokenizationSuccess_sendsAnalyticsEvent() {
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

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .tokenizeSuccess(
                fromJSON(
                    JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
                )
            )
            .build()

        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)

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
    fun tokenize_whenResultOK_onConfigurationError_returnsError() {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        val configError = Exception("config error")
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
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)

        val slot = slot<LocalPaymentResult>()
        verify { localPaymentTokenizeCallback.onLocalPaymentResult(capture(slot)) }

        val result = slot.captured
        assert(result is LocalPaymentResult.Failure)
        val exception = (result as LocalPaymentResult.Failure).error
        assertEquals(configError, exception)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOKAndUserCancels_notifiesCallbackAndSendsAnalyticsEvent() {
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)
        every { browserSwitchResult.requestMetadata } returns JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
        val webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled"
        every { browserSwitchResult.returnUrl } returns Uri.parse(webUrl)

        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)
        val sut = LocalPaymentClient(
            braintreeClient, dataCollector,
            localPaymentApi, analyticsParamRepository
        )

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback)

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
    fun onBrowserSwitchResult_sends_the_correct_value_of_hasUserLocationConsent_to_getClientMetadataId() {
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

        val request = this.idealLocalPaymentRequest
        val sut = LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository
        )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(browserSwitchResult)

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback)
        sut.tokenize(
            activity,
            localPaymentAuthResult,
            mockk(relaxed = true)
        )

        verify {
            dataCollector.getClientMetadataId(
                any(),
                payPalEnabledConfig,
                eq(true)
            )
        }
    }

    private val idealLocalPaymentRequest: LocalPaymentRequest
        get() {
            val address = PostalAddress()
            address.streetAddress = "836486 of 22321 Park Lake"
            address.extendedAddress = "Apt 2"
            address.countryCodeAlpha2 = "NL"
            address.locality = "Den Haag"
            address.region = "CA"
            address.postalCode = "2585 GJ"

            return LocalPaymentRequest(
                true,
                address,
                "1.10",
                "bank-id-code",
                "EUR",
                "My Brand!",
                "jon@getbraintree.com",
                "Jon",
                "local-merchant-account-id",
                "ideal",
                "NL",
                "639847934",
                true,
                "Doe"
            )
        }
}
