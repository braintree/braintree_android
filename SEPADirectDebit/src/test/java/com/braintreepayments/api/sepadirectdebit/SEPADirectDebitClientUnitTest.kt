package com.braintreepayments.api.sepadirectdebit

import android.net.Uri
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.sepadirectdebit.SEPADirectDebitNonce.Companion.fromJSON
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import io.mockk.*
@RunWith(RobolectricTestRunner::class)
class SEPADirectDebitClientUnitTest {

    private var braintreeClient: BraintreeClient = MockkBraintreeClientBuilder()
        .returnUrlScheme("com.example")
        .build()
    private var createMandateResult: CreateMandateResult = CreateMandateResult(
        "http://www.example.com",
        "1234",
        "fake-customer-id",
        "fake-bank-reference-token",
        SEPADirectDebitMandateType.valueOf("ONE_OFF")
    )
    private var sepaDirectDebitRequest: SEPADirectDebitRequest = SEPADirectDebitRequest()
    private var sepaTokenizeCallback: SEPADirectDebitTokenizeCallback = mockk(relaxed = true)
    private var paymentAuthRequestCallback: SEPADirectDebitPaymentAuthRequestCallback = mockk(relaxed = true)

    @Before
    fun beforeEach() {
        braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("com.example")
            .build()
        clearMocks(sepaTokenizeCallback, paymentAuthRequestCallback)
    }

    @Test
    @Throws(JSONException::class)
    fun createPaymentAuthRequest_onCreateMandateRequestSuccess_callsBackSEPAResponse_andSendsAnalytics() {
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .createMandateResultSuccess(createMandateResult)
            .build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)

        sut.createPaymentAuthRequest(sepaDirectDebitRequest!!, paymentAuthRequestCallback!!)

        verify {
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.TOKENIZE_STARTED,
                any(),
                true
            )
        }

        val captor = slot<SEPADirectDebitPaymentAuthRequest>()
        verify {
            paymentAuthRequestCallback?.onSEPADirectDebitPaymentAuthResult(capture(captor))
        }

        val paymentAuthRequest = captor.captured
        Assert.assertTrue(paymentAuthRequest is SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)
        val params = (paymentAuthRequest as SEPADirectDebitPaymentAuthRequest.ReadyToLaunch).requestParams

        verify {
            braintreeClient?.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED, any(), true)
            braintreeClient?.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_CHALLENGE_REQUIRED, any(), true)
        }

        val browserSwitchOptions = params.browserSwitchOptions
        Assert.assertEquals(Uri.parse("http://www.example.com"), browserSwitchOptions.getUrl())
        Assert.assertEquals("com.example", browserSwitchOptions.getReturnUrlScheme())
        Assert.assertEquals(
            BraintreeRequestCodes.SEPA_DEBIT.code.toLong(),
            browserSwitchOptions.getRequestCode().toLong()
        )
        val metadata = browserSwitchOptions.getMetadata()
        Assert.assertEquals("1234", metadata!!.get("ibanLastFour"))
        Assert.assertEquals("fake-customer-id", metadata.get("customerId"))
        Assert.assertEquals("fake-bank-reference-token", metadata.get("bankReferenceToken"))
        Assert.assertEquals("ONE_OFF", metadata.get("mandateType"))
    }

    @Test
    @Throws(JSONException::class)
    fun createPaymentAuthRequest_whenMandateApproved_onTokenizeSuccess_callsBackWithNonce_andSendsAnalytics() {
        createMandateResult = CreateMandateResult(
            "null",
            "1234",
            "fake-customer-id",
            "fake-bank-reference-token",
            SEPADirectDebitMandateType.valueOf("ONE_OFF")
        )

        val nonce = fromJSON(
            JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
        )
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .createMandateResultSuccess(createMandateResult)
            .tokenizeSuccess(nonce)
            .build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)

        sut.createPaymentAuthRequest(sepaDirectDebitRequest!!, paymentAuthRequestCallback!!)

        val captor = slot<SEPADirectDebitPaymentAuthRequest>()
        verify {
            paymentAuthRequestCallback?.onSEPADirectDebitPaymentAuthResult(capture(captor))
        }

        val paymentAuthRequest = captor.captured
        Assert.assertTrue(paymentAuthRequest is SEPADirectDebitPaymentAuthRequest.LaunchNotRequired)
        Assert.assertEquals(
            (paymentAuthRequest as SEPADirectDebitPaymentAuthRequest.LaunchNotRequired).nonce,
            nonce
        )
        verify {
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED,
                any(),
                true
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_onCreateMandateRequestSuccess_whenApprovalURLInvalid_callsBackError() {
        createMandateResult = CreateMandateResult(
            "",
            "1234",
            "fake-customer-id",
            "fake-bank-reference-token",
            SEPADirectDebitMandateType.valueOf("ONE_OFF")
        )

        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .createMandateResultSuccess(createMandateResult)
            .build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)

        sut.createPaymentAuthRequest(sepaDirectDebitRequest!!, paymentAuthRequestCallback!!)

        val captor = slot<SEPADirectDebitPaymentAuthRequest>()
        verify {
            paymentAuthRequestCallback?.onSEPADirectDebitPaymentAuthResult(capture(captor))
        }

        val paymentAuthRequest = captor.captured
        Assert.assertTrue(paymentAuthRequest is SEPADirectDebitPaymentAuthRequest.Failure)
        val error = (paymentAuthRequest as SEPADirectDebitPaymentAuthRequest.Failure).error
        Assert.assertTrue(error is BraintreeException)
        Assert.assertEquals("An unexpected error occurred.", error.message)

        val createMandateFailedSlot = slot<AnalyticsEventParams>()
        val tokenizeFailedSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED,
                capture(createMandateFailedSlot),
                true
            )
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.TOKENIZE_FAILED,
                capture(tokenizeFailedSlot),
                true
            )
        }
        Assert.assertEquals(
            "An unexpected error occurred.",
            createMandateFailedSlot.captured.errorDescription
        )
        Assert.assertEquals(
            "An unexpected error occurred.",
            tokenizeFailedSlot.captured.errorDescription
        )
    }

    @Test
    fun createPaymentAuthRequest_whenMandateApproved_callsTokenizeAndSendsAnalytics() {
        createMandateResult = CreateMandateResult(
            "null",
            "1234",
            "fake-customer-id",
            "fake-bank-reference-token",
            SEPADirectDebitMandateType.valueOf("ONE_OFF")
        )

        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .createMandateResultSuccess(createMandateResult)
            .build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)

        sut.createPaymentAuthRequest(sepaDirectDebitRequest!!, paymentAuthRequestCallback!!)

        verify {
            braintreeClient?.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_STARTED, any(), true)
            braintreeClient?.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED, any(), true)
            sepaDirectDebitApi.tokenize(
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF",
                any()
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_onCreateMandateError_returnsErrorToListener_andSendsAnalytics() {
        val error = Exception("error")
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .createMandateError(error)
            .build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)

        sut.createPaymentAuthRequest(sepaDirectDebitRequest!!, paymentAuthRequestCallback!!)

        val captor = slot<SEPADirectDebitPaymentAuthRequest>()
        verify {
            paymentAuthRequestCallback?.onSEPADirectDebitPaymentAuthResult(capture(captor))
        }

        val paymentAuthRequest = captor.captured
        Assert.assertTrue(paymentAuthRequest is SEPADirectDebitPaymentAuthRequest.Failure)
        val actualError = (paymentAuthRequest as SEPADirectDebitPaymentAuthRequest.Failure).error
        Assert.assertEquals(error, actualError)

        val createMandateFailedSlot = slot<AnalyticsEventParams>()
        val tokenizeFailedSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED,
                capture(createMandateFailedSlot),
                true
            )
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.TOKENIZE_FAILED,
                capture(tokenizeFailedSlot),
                true
            )
        }
        Assert.assertEquals(error.message, createMandateFailedSlot.captured.errorDescription)
        Assert.assertEquals(error.message, tokenizeFailedSlot.captured.errorDescription)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenDeepLinkContainsSuccess_callsTokenize_andSendsAnalytics() {
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder().build()

        val metadata = JSONObject()
            .put("ibanLastFour", "1234")
            .put("customerId", "customer-id")
            .put("bankReferenceToken", "bank-reference-token")
            .put("mandateType", "ONE_OFF")

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()
        val returnUri = Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true")
        every { browserSwitchResult.returnUrl } returns returnUri
        every { browserSwitchResult.requestMetadata } returns metadata

        braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)
        val sepaBrowserSwitchResult = SEPADirectDebitPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback!!)

        verify {
            sepaDirectDebitApi.tokenize(
                "1234",
                "customer-id",
                "bank-reference-token",
                "ONE_OFF",
                any()
            )
            braintreeClient?.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_SUCCEEDED, any(), true)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_onTokenizeSuccess_callsBackNonce_andSendsAnalytics() {
        val nonce = fromJSON(
            JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
        )
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .tokenizeSuccess(nonce)
            .build()

        val metadata = JSONObject()
            .put("ibanLastFour", "1234")
            .put("customerId", "customer-id")
            .put("bankReferenceToken", "bank-reference-token")
            .put("mandateType", "ONE_OFF")

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()
        val returnUri = Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true")
        every { browserSwitchResult.returnUrl } returns returnUri
        every { browserSwitchResult.requestMetadata } returns metadata

        braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)
        val sepaBrowserSwitchResult = SEPADirectDebitPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback!!)

        val captor = slot<SEPADirectDebitResult>()
        verify {
            sepaTokenizeCallback?.onSEPADirectDebitResult(capture(captor))
        }

        val result = captor.captured
        Assert.assertTrue(result is SEPADirectDebitResult.Success)
        Assert.assertEquals(nonce, (result as SEPADirectDebitResult.Success).nonce)

        verify {
            braintreeClient?.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED, any(), true)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_onTokenizeFailure_callsBackError_andSendsAnalytics() {
        val exception = Exception("tokenize error")
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .tokenizeError(exception)
            .build()

        val metadata = JSONObject()
            .put("ibanLastFour", "1234")
            .put("customerId", "customer-id")
            .put("bankReferenceToken", "bank-reference-token")
            .put("mandateType", "ONE_OFF")

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()
        val returnUri = Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true")
        every { browserSwitchResult.returnUrl } returns returnUri
        every { browserSwitchResult.requestMetadata } returns metadata

        braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)
        val sepaBrowserSwitchResult = SEPADirectDebitPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback!!)

        val captor = slot<SEPADirectDebitResult>()
        verify {
            sepaTokenizeCallback?.onSEPADirectDebitResult(capture(captor))
        }

        val result = captor.captured
        Assert.assertTrue(result is SEPADirectDebitResult.Failure)
        Assert.assertEquals(exception, (result as SEPADirectDebitResult.Failure).error)

        val tokenizeFailedSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.TOKENIZE_FAILED,
                capture(tokenizeFailedSlot),
                true
            )
            sepaDirectDebitApi.tokenize(
                "1234",
                "customer-id",
                "bank-reference-token",
                "ONE_OFF",
                any()
            )
        }
        Assert.assertEquals(exception.message, tokenizeFailedSlot.captured.errorDescription)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_onTokenizeSuccess_callsBackResult() {
        val nonce = fromJSON(
            JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
        )
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder()
            .tokenizeSuccess(nonce)
            .build()

        val metadata = JSONObject()
            .put("ibanLastFour", "1234")
            .put("customerId", "customer-id")
            .put("bankReferenceToken", "bank-reference-token")
            .put("mandateType", "ONE_OFF")

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()
        val returnUri = Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true")
        every { browserSwitchResult.returnUrl } returns returnUri
        every { browserSwitchResult.requestMetadata } returns metadata

        braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)
        val sepaBrowserSwitchResult = SEPADirectDebitPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback!!)

        val captor = slot<SEPADirectDebitResult>()
        verify {
            sepaTokenizeCallback?.onSEPADirectDebitResult(capture(captor))
        }

        val result = captor.captured
        Assert.assertTrue(result is SEPADirectDebitResult.Success)
        Assert.assertEquals(nonce, (result as SEPADirectDebitResult.Success).nonce)
    }

    @Test
    fun tokenize_whenDeepLinkContainsCancel_callsBackError_andSendsAnalytics() {
        val sepaDirectDebitApi = MockkSEPADirectDebitApiBuilder().build()

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()
        every { browserSwitchResult.returnUrl } returns Uri.parse(
            "com.braintreepayments.demo.braintree://sepa/cancel?error_code=internal_error"
        )

        braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = SEPADirectDebitClient(braintreeClient!!, sepaDirectDebitApi)
        val sepaBrowserSwitchResult = SEPADirectDebitPaymentAuthResult.Success(browserSwitchResult)

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback!!)

        val captor = slot<SEPADirectDebitResult>()
        verify {
            sepaTokenizeCallback?.onSEPADirectDebitResult(capture(captor))
        }

        val result = captor.captured
        Assert.assertTrue(result is SEPADirectDebitResult.Cancel)

        verify {
            braintreeClient?.sendAnalyticsEvent(
                SEPADirectDebitAnalytics.CHALLENGE_CANCELED,
                any(),
                true
            )
        }
    }
}
