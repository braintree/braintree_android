package com.braintreepayments.api.localpayment

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.core.ConfigurationException
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.localpayment.LocalPaymentNonce.Companion.fromJSON
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class LocalPaymentClientUnitTest {
    private var activity: FragmentActivity? = null
    private var localPaymentAuthCallback: LocalPaymentAuthCallback? = null
    private var localPaymentTokenizeCallback: LocalPaymentTokenizeCallback? = null
    private var braintreeClient: BraintreeClient? = null
    private var dataCollector: DataCollector? = null
    private var localPaymentApi: LocalPaymentApi? = null
    private var localPaymentAuthRequestParams: LocalPaymentAuthRequestParams? = null

    private var payPalEnabledConfig: Configuration? = null
    private var payPalDisabledConfig: Configuration? = null

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        activity = Mockito.mock(FragmentActivity::class.java)
        localPaymentAuthCallback = Mockito.mock(LocalPaymentAuthCallback::class.java)
        localPaymentTokenizeCallback = Mockito.mock(
            LocalPaymentTokenizeCallback::class.java
        )

        braintreeClient =
            MockBraintreeClientBuilder().configuration(
                fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
            ).build()
        dataCollector = Mockito.mock(DataCollector::class.java)
        localPaymentApi = Mockito.mock(LocalPaymentApi::class.java)
        localPaymentAuthRequestParams = Mockito.mock(
            LocalPaymentAuthRequestParams::class.java
        )
        Mockito.`when`(localPaymentAuthRequestParams!!.approvalUrl).thenReturn("https://")
        Mockito.`when`(localPaymentAuthRequestParams!!.request).thenReturn(idealLocalPaymentRequest)

        payPalEnabledConfig = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        payPalDisabledConfig = fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL)
    }

    @Test
    fun createPaymentAuthRequest_sendsPaymentStartedEvent() {
        val sut =
            LocalPaymentClient(braintreeClient!!, dataCollector!!, localPaymentApi!!)
        sut.createPaymentAuthRequest(idealLocalPaymentRequest, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient)?.sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED)
    }

    @Test
    fun createPaymentAuthRequest_sendsPaymentFailedEvent_forNullRequest() {
        val sut = LocalPaymentClient(
            braintreeClient!!,
            dataCollector!!,
            localPaymentApi!!
        )
        sut.createPaymentAuthRequest(null, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient)?.sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            AnalyticsEventParams()
        )
    }

    @Test
    fun createPaymentAuthRequest_sendsPaymentFailedEvent_forNullGetPaymentType() {
        val request = idealLocalPaymentRequest
        request.paymentType = null

        val sut = LocalPaymentClient(
            braintreeClient!!,
            dataCollector!!,
            localPaymentApi!!
        )
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient)?.sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            AnalyticsEventParams()
        )
    }

    @Test
    fun createPaymentAuthRequest_createsPaymentMethodWithLocalPaymentApi() {
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build()
        val localPaymentApi = MockLocalPaymentApiBuilder().build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        Mockito.verify(localPaymentApi).createPaymentMethod(
            ArgumentMatchers.same(request),
            ArgumentMatchers.any(
                LocalPaymentInternalAuthRequestCallback::class.java
            )
        )
    }

    @Test
    fun createPaymentAuthRequest_success_forwardsResultToCallback() {
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build()

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        assertEquals(localPaymentAuthRequestParams, params)
    }

    @Test
    fun createPaymentAuthRequest_success_sendsAnalyticsEvents() {
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build()
        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(localPaymentAuthRequestParams)
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
            AnalyticsEventParams()
        )
    }

    @Test
    fun createPaymentAuthRequest_configurationFetchError_forwardsErrorToCallback() {
        val configException = Exception(("Configuration not fetched"))
        val braintreeClient = MockBraintreeClientBuilder()
            .configurationError(configException)
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi!!
            )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertEquals(exception, configException)
    }

    @Test
    fun createPaymentAuthRequest_onLocalPaymentApiError_sendsAnalyticsEvents() {
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build()

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodError(Exception("error"))
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            AnalyticsEventParams()
        )
    }

    @Test
    fun createPaymentAuthRequest_whenPayPalDisabled_returnsErrorToCallback() {
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalDisabledConfig)
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi!!
            )
        val request = idealLocalPaymentRequest
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertTrue(exception is ConfigurationException)
        assertEquals("Local payments are not enabled for this merchant.", exception.message)
    }

    @Test
    fun createPaymentAuthRequest_whenAmountIsNull_returnsErrorToCallback() {
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )
        val request = idealLocalPaymentRequest
        request.amount = null

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertTrue(exception is BraintreeException)
        assertEquals(
            "LocalPaymentRequest is invalid, paymentType and amount are required.",
            exception.message
        )
    }

    @Test
    fun createPaymentAuthRequest_whenPaymentTypeIsNull_returnsErrorToCallback() {
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )
        val request = idealLocalPaymentRequest
        request.paymentType = null

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertTrue(exception is BraintreeException)
        assertEquals(
            "LocalPaymentRequest is invalid, paymentType and amount are required.",
            exception.message
        )
    }

    @Test
    fun createPaymentAuthRequest_whenLocalPaymentRequestIsNull_returnsErrorToCallback() {
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )

        sut.createPaymentAuthRequest(null, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertTrue(exception is BraintreeException)
        assertEquals("A LocalPaymentRequest is required.", exception.message)
    }

    @Test
    fun createPaymentAuthRequest_whenCallbackIsNull_throwsError() {
        val request = idealLocalPaymentRequest
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )

        try {
            sut.createPaymentAuthRequest(request, null)
           fail("Should throw")
        } catch (exception: RuntimeException) {
            assertEquals("A LocalPaymentAuthRequestCallback is required.", exception.message)
        }
    }

    @Test
    fun createPaymentAuthRequest_whenCreatePaymentMethodError_returnsErrorToCallback() {
        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodError(Exception("error"))
            .build()
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi
            )

        sut.createPaymentAuthRequest(
            idealLocalPaymentRequest,
            localPaymentAuthCallback!!
        )

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.Failure)
        val exception = (paymentAuthRequest as LocalPaymentAuthRequest.Failure).error
        assertTrue(exception is BraintreeException)
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

        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi
            )

        sut.createPaymentAuthRequest(idealLocalPaymentRequest, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        assertEquals(localPaymentAuthRequestParams, params)
    }

    @Test
    fun createPaymentAuthRequest_success_withEmptyPaymentId_sendsAnalyticsEvents() {
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build()
        val request = idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "")

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(transaction)
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )
        sut.createPaymentAuthRequest(idealLocalPaymentRequest, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED)
        Mockito.verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
            AnalyticsEventParams()
        )
    }


    @Test
    fun createPaymentAuthRequest_success_withPaymentId_sendsAnalyticsEvents() {
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build()
        val request = idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction =
            LocalPaymentAuthRequestParams(request, approvalUrl, "some-paypal-context-id")

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(transaction)
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )
        sut.createPaymentAuthRequest(idealLocalPaymentRequest, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED)
        val params = AnalyticsEventParams()
        params.payPalContextId = "some-paypal-context-id"
        Mockito.verify(braintreeClient)
            .sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED, params)
    }

    @Test
    @Throws(JSONException::class)
    fun buildBrowserSwitchOptions_returnsLocalPaymentResultWithBrowserSwitchOptions() {
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )

        val request = idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        val browserSwitchOptions = params.browserSwitchOptions
        assertEquals(
            BraintreeRequestCodes.LOCAL_PAYMENT.code,
            browserSwitchOptions!!.requestCode
        )
        assertEquals(
            Uri.parse("https://sample.com/approval?token=sample-token"),
            browserSwitchOptions.url
        )
        TestCase.assertFalse(browserSwitchOptions.isLaunchAsNewTask)

        val metadata = browserSwitchOptions.metadata
        val expectedMetadata = JSONObject()
            .put("merchant-account-id", "local-merchant-account-id")
            .put("payment-type", "ideal")
            .put("has-user-location-consent", true)

        JSONAssert.assertEquals(expectedMetadata, metadata, true)
    }

    @Test
    fun buildBrowserSwitchOptions_withDefaultDeepLinkHandlerEnabled_startsBrowserSwitchAsNewTaskWithProperRequestCode() {
        Mockito.`when`(braintreeClient!!.launchesBrowserSwitchAsNewTask()).thenReturn(true)
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )

        val request = idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentAuthRequest::class.java
        )
        Mockito.verify(localPaymentAuthCallback)?.onLocalPaymentAuthRequest(captor.capture())

        val paymentAuthRequest = captor.value
        assertTrue(paymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams
        val browserSwitchOptions = params.browserSwitchOptions

        assertTrue(browserSwitchOptions!!.isLaunchAsNewTask)
    }

    @Test
    fun buildBrowserSwitchOptions_sendsAnalyticsEvents() {
        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )

        val request = idealLocalPaymentRequest
        val approvalUrl = "https://sample.com/approval?token=sample-token"
        val transaction = LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id")

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback!!)

        Mockito.verify(braintreeClient)?.sendAnalyticsEvent(
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
            AnalyticsEventParams()
        )
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOK_uriNull_notifiesCallbackOfErrorAlongWithAnalyticsEvent() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
            Mockito.mock(
                BrowserSwitchFinalResult.Success::class.java
            )
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(
            LocalPaymentAuthResultInfo(browserSwitchResult)
        )

        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )

        sut.tokenize(activity!!, localPaymentAuthResult, localPaymentTokenizeCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentResult::class.java
        )
        Mockito.verify(localPaymentTokenizeCallback)?.onLocalPaymentResult(
            captor.capture()
        )

        val result = captor.value
        assertTrue(result is LocalPaymentResult.Failure)
        val exception = (result as LocalPaymentResult.Failure).error
        assertTrue(exception is BraintreeException)

        val expectedMessage = "LocalPayment encountered an error, return URL is invalid."
        assertEquals(expectedMessage, exception.message)

        Mockito.verify(braintreeClient)?.sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            AnalyticsEventParams()
        )
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenPostFailure_notifiesCallbackOfErrorAlongWithAnalyticsEvent() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
             Mockito.mock(BrowserSwitchFinalResult.Success::class.java)
        Mockito.`when`<JSONObject>(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id")
        )

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        Mockito.`when`(browserSwitchResult.returnUrl).thenReturn(Uri.parse(webUrl))
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(
            LocalPaymentAuthResultInfo(browserSwitchResult)
        )

        val postError = Exception("POST failed")
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .sendPOSTErrorResponse(postError)
            .sessionId("sample-session-id")
            .integration(IntegrationType.CUSTOM)
            .build()

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .tokenizeError(postError)
            .build()

        Mockito.`when`(dataCollector!!.getClientMetadataId(activity, payPalEnabledConfig, false))
            .thenReturn(
                "sample-correlation-id"
            )

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )

        sut.tokenize(activity!!, localPaymentAuthResult, localPaymentTokenizeCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentResult::class.java
        )
        Mockito.verify(localPaymentTokenizeCallback)?.onLocalPaymentResult(
            captor.capture()
        )

        val result = captor.value
        assertTrue(result is LocalPaymentResult.Failure)
        val exception = (result as LocalPaymentResult.Failure).error
        assertEquals(postError, exception)
        Mockito.verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            AnalyticsEventParams()
        )
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOKAndSuccessful_tokenizesWithLocalPaymentApi() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
             Mockito.mock(BrowserSwitchFinalResult.Success::class.java)

        Mockito.`when`<JSONObject>(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id")
        )

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        Mockito.`when`(browserSwitchResult.returnUrl).thenReturn(Uri.parse(webUrl))
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(
            LocalPaymentAuthResultInfo(browserSwitchResult)
        )

        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .sessionId("sample-session-id")
            .integration(IntegrationType.CUSTOM)
            .build()
        Mockito.`when`(dataCollector!!.getClientMetadataId(activity, payPalEnabledConfig, false))
            .thenReturn(
                "sample-correlation-id"
            )

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi!!
            )

        sut.tokenize(activity!!, localPaymentAuthResult, localPaymentTokenizeCallback!!)

        Mockito.verify(localPaymentApi)?.tokenize(
            ArgumentMatchers.eq("local-merchant-account-id"), ArgumentMatchers.eq(webUrl),
            ArgumentMatchers.eq("sample-correlation-id"), ArgumentMatchers.any(
                LocalPaymentInternalTokenizeCallback::class.java
            )
        )
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOKAndTokenizationSucceeds_sendsResultToCallback() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
             Mockito.mock(BrowserSwitchFinalResult.Success::class.java)

        Mockito.`when`<JSONObject>(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id")
        )

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        Mockito.`when`(browserSwitchResult.returnUrl).thenReturn(Uri.parse(webUrl))
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(
            LocalPaymentAuthResultInfo(browserSwitchResult)
        )

        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .integration(IntegrationType.CUSTOM)
            .sessionId("session-id")
            .build()
        Mockito.`when`(
            dataCollector!!.getClientMetadataId(
                ArgumentMatchers.any(
                    Context::class.java
                ),
                ArgumentMatchers.same(payPalEnabledConfig), ArgumentMatchers.eq(false)
            )
        ).thenReturn("client-metadata-id")

        val successNonce = fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
        )
        val localPaymentApi = MockLocalPaymentApiBuilder()
            .tokenizeSuccess(successNonce)
            .build()

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )

        sut.tokenize(activity!!, localPaymentAuthResult, localPaymentTokenizeCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentResult::class.java
        )
        Mockito.verify(localPaymentTokenizeCallback)?.onLocalPaymentResult(
            captor.capture()
        )

        val result = captor.value
        assertTrue(result is LocalPaymentResult.Success)
        val nonce = (result as LocalPaymentResult.Success).nonce
        assertEquals(successNonce, nonce)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOKAndTokenizationSuccess_sendsAnalyticsEvent() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
             Mockito.mock(BrowserSwitchFinalResult.Success::class.java)

        Mockito.`when`<JSONObject>(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id")
        )

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        Mockito.`when`(browserSwitchResult.returnUrl).thenReturn(Uri.parse(webUrl))
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(
            LocalPaymentAuthResultInfo(browserSwitchResult)
        )
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build()

        val localPaymentApi = MockLocalPaymentApiBuilder()
            .tokenizeSuccess(
                fromJSON(
                    JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
                )
            )
            .build()

        Mockito.`when`(
            dataCollector!!.getClientMetadataId(
                ArgumentMatchers.any(
                    Context::class.java
                ),
                ArgumentMatchers.same(payPalEnabledConfig), ArgumentMatchers.eq(false)
            )
        ).thenReturn("client-metadata-id")


        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi
            )

        sut.tokenize(activity!!, localPaymentAuthResult, localPaymentTokenizeCallback!!)

        Mockito.verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_SUCCEEDED,
            AnalyticsEventParams()
        )
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOK_onConfigurationError_returnsError() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
             Mockito.mock(BrowserSwitchFinalResult.Success::class.java)

        Mockito.`when`<JSONObject>(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id")
        )

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        Mockito.`when`(browserSwitchResult.returnUrl).thenReturn(Uri.parse(webUrl))
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(
            LocalPaymentAuthResultInfo(browserSwitchResult)
        )

        val configError = Exception("config error")
        val braintreeClient = MockBraintreeClientBuilder()
            .configurationError(configError)
            .sessionId("sample-session-id")
            .integration(IntegrationType.CUSTOM)
            .build()
        Mockito.`when`(dataCollector!!.getClientMetadataId(activity, payPalEnabledConfig, true))
            .thenReturn(
                "sample-correlation-id"
            )

        val sut =
            LocalPaymentClient(
                braintreeClient, dataCollector!!,
                localPaymentApi!!
            )

        sut.tokenize(activity!!, localPaymentAuthResult, localPaymentTokenizeCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentResult::class.java
        )
        Mockito.verify(localPaymentTokenizeCallback)?.onLocalPaymentResult(
            captor.capture()
        )

        val result = captor.value
        assertTrue(result is LocalPaymentResult.Failure)
        val exception = (result as LocalPaymentResult.Failure).error
        assertEquals(configError, exception)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenResultOKAndUserCancels_notifiesCallbackAndSendsAnalyticsEvent() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
             Mockito.mock(BrowserSwitchFinalResult.Success::class.java)

        Mockito.`when`<JSONObject>(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id")
        )

        val webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled"
        Mockito.`when`(browserSwitchResult.returnUrl).thenReturn(Uri.parse(webUrl))
        val localPaymentAuthResult = LocalPaymentAuthResult.Success(
            LocalPaymentAuthResultInfo(browserSwitchResult)
        )

        val sut =
            LocalPaymentClient(
                braintreeClient!!, dataCollector!!,
                localPaymentApi!!
            )

        sut.tokenize(activity!!, localPaymentAuthResult, localPaymentTokenizeCallback!!)

        val captor = ArgumentCaptor.forClass(
            LocalPaymentResult::class.java
        )
        Mockito.verify(localPaymentTokenizeCallback)?.onLocalPaymentResult(
            captor.capture()
        )

        val result = captor.value
        assertTrue(result is LocalPaymentResult.Cancel)
        Mockito.verify(braintreeClient)?.sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_CANCELED,
            AnalyticsEventParams()
        )
    }

    @Test
    @Throws(JSONException::class)
    fun onBrowserSwitchResult_sends_the_correct_value_of_hasUserLocationConsent_to_getClientMetadataId() {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
            Mockito.mock(BrowserSwitchFinalResult.Success::class.java)

        Mockito.`when`<JSONObject>(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id")
                .put("has-user-location-consent", true)
        )

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        Mockito.`when`(browserSwitchResult.returnUrl).thenReturn(Uri.parse(webUrl))
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .integration(IntegrationType.CUSTOM)
            .sessionId("session-id")
            .build()
        Mockito.`when`(
            dataCollector!!.getClientMetadataId(
                ArgumentMatchers.any(
                    Context::class.java
                ), ArgumentMatchers.same(payPalEnabledConfig), ArgumentMatchers.anyBoolean()
            )
        ).thenReturn("client-metadata-id")
        
        val request = idealLocalPaymentRequest
        val sut = LocalPaymentClient(braintreeClient, dataCollector!!, localPaymentApi!!)
        val localPaymentAuthResult =
            LocalPaymentAuthResult.Success(LocalPaymentAuthResultInfo(browserSwitchResult))

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback!!)

        sut.tokenize(activity!!, localPaymentAuthResult, Mockito.mock())

        Mockito.verify(dataCollector)?.getClientMetadataId(
            ArgumentMatchers.any(),
            ArgumentMatchers.same(payPalEnabledConfig),
            ArgumentMatchers.eq(true)
        )
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

            val request = LocalPaymentRequest(true)
            request.paymentType = "ideal"
            request.amount = "1.10"
            request.address = address
            request.phone = "639847934"
            request.email = "jon@getbraintree.com"
            request.givenName = "Jon"
            request.surname = "Doe"
            request.isShippingAddressRequired = false
            request.merchantAccountId = "local-merchant-account-id"
            request.currencyCode = "EUR"
            request.paymentTypeCountryCode = "NL"
            request.displayName = "My Brand!"

            return request
        }
}
