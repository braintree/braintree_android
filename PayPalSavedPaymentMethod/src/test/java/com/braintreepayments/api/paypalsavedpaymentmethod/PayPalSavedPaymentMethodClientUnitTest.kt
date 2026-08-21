package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalBetaApi::class)
@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodClientUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val payPalClient = mockk<PayPalClient>(relaxed = true)

    private fun mockConfiguration(env: String = "production"): Configuration =
        mockk(relaxed = true) {
            every { environment } returns env
        }

    private fun mockMerchantRepository(paymentMethodIdJwt: String?): MerchantRepository {
        val clientToken = mockk<ClientToken>(relaxed = true) {
            every { this@mockk.paymentMethodIdJwt } returns paymentMethodIdJwt
        }
        return mockk(relaxed = true) {
            every { authorization } returns clientToken
        }
    }

    @Test
    fun fetchFI_withJwt_postsStickyFiBodyAndReturnsSuccess() = runTest(testDispatcher) {
        val responseJson = """{"data":{"paypalFundingInstrumentDetails":{"payer":null,"paymentMethods":[]}}}"""
        val bodySlot = slot<JSONObject>()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(capture(bodySlot)) } returns responseJson

        val sut = PayPalSavedPaymentMethodClient(
            braintreeClient,
            payPalClient,
            merchantRepository = mockMerchantRepository("pmid-jwt")
        )

        val result = sut.fetchFI()

        assertEquals(
            GetPayPalSavedPaymentMethodGraphQLBody.stickyFi("pmid-jwt").toString(),
            bodySlot.captured.toString()
        )
        assertTrue(result is PayPalSavedPaymentMethodSummaryResult.Success)
    }

    @Test
    fun fetchFI_whenJwtMissing_returnsFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = PayPalSavedPaymentMethodClient(
            braintreeClient,
            payPalClient,
            merchantRepository = mockMerchantRepository(null)
        )

        val result = sut.fetchFI()

        assertTrue(result is PayPalSavedPaymentMethodSummaryResult.Failure)
        val error = (result as PayPalSavedPaymentMethodSummaryResult.Failure).error
        assertTrue(error is PayPalSavedPaymentMethodSummaryException)
        assertEquals(
            PayPalSavedPaymentMethodSummaryException.MISSING_PAYMENT_METHOD_ID_JWT,
            error.message
        )
    }

    @Test
    fun refetchFI_postsApprovedCheckoutBodyAndReturnsSuccess() = runTest(testDispatcher) {
        val responseJson = """{"data":{"paypalFundingInstrumentDetails":{"payer":null,"paymentMethods":[]}}}"""
        val bodySlot = slot<JSONObject>()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(capture(bodySlot)) } returns responseJson

        val sut = PayPalSavedPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.refetchFI("order-123")

        assertEquals(
            GetPayPalSavedPaymentMethodGraphQLBody.fromApprovedCheckout("order-123").toString(),
            bodySlot.captured.toString()
        )
        assertTrue(result is PayPalSavedPaymentMethodSummaryResult.Success)
    }

    @Test
    fun fetchFI_whenGraphQLReturnsErrors_returnsFailurePreservingErrorClass() = runTest(testDispatcher) {
        val responseJson = """
            {"errors":[{"message":"PayPal access token not found for merchant account.",
              "extensions":{"errorClass":"AUTHENTICATION","errorType":"developer_error"}}],
             "data":{"paypalFundingInstrumentDetails":null}}
        """.trimIndent()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(any()) } returns responseJson

        val sut = PayPalSavedPaymentMethodClient(
            braintreeClient,
            payPalClient,
            merchantRepository = mockMerchantRepository("pmid-jwt")
        )

        val result = sut.fetchFI()

        assertTrue(result is PayPalSavedPaymentMethodSummaryResult.Failure)
        val error = (result as PayPalSavedPaymentMethodSummaryResult.Failure).error
        assertTrue(error is PayPalSavedPaymentMethodSummaryException)
        assertEquals("AUTHENTICATION", (error as PayPalSavedPaymentMethodSummaryException).errorClass)
    }

    @Test
    fun fetchFI_whenNetworkError_returnsFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(any()) } throws IOException("network down")

        val sut = PayPalSavedPaymentMethodClient(
            braintreeClient,
            payPalClient,
            merchantRepository = mockMerchantRepository("pmid-jwt")
        )

        val result = sut.fetchFI()

        assertTrue(result is PayPalSavedPaymentMethodSummaryResult.Failure)
        assertTrue((result as PayPalSavedPaymentMethodSummaryResult.Failure).error is IOException)
    }

    @Test
    fun fetchCreditPresentmentMessages_postsRequestBodyToCreditUrlAndReturnsSuccess() =
        runTest(testDispatcher) {
            val responseJson = """
                {"messages":[{"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ",
                  "content":{"main_items":[{"type":"TEXT","text":"As low as \${'$'}10/mo"}],
                  "action_items":[{"type":"LINK","text":"Learn more","click_url":"https://paypal.com/learn"}]},
                  "analytics":{"impression_url":"https://paypal.com/impression"}},
                  "selection_reasons":[{"code":"DEFAULT_PREFERRED","description":"default"}]}]}
            """.trimIndent()
            val urlSlot = slot<String>()
            val bodySlot = slot<String>()
            val braintreeClient = MockkBraintreeClientBuilder()
                .configurationSuccess(mockConfiguration())
                .build()
            coEvery {
                braintreeClient.sendPOST(url = capture(urlSlot), data = capture(bodySlot))
            } returns responseJson

            val sut = PayPalSavedPaymentMethodClient(braintreeClient, payPalClient)
            val request = PayPalCreditMessagingRequest.forAmount(currencyCode = "USD", value = "55.00")

            val result = sut.fetchCreditPresentmentMessages(request)

            assertEquals(
                "https://api.paypal.com/v2/credit/fetch-presentment-messages",
                urlSlot.captured
            )
            assertEquals(request.build().toString(), bodySlot.captured)
            requireNotNull(result)
            assertEquals("As low as \$10/mo", result.message)
            assertEquals("Learn more", result.learnMoreText)
            assertEquals("https://paypal.com/learn", result.learnMoreUrl)
        }

    @Test
    fun fetchCreditPresentmentMessages_withImageAndDisclaimerItems_buildsFlattenedMessage() =
        runTest(testDispatcher) {
            val responseJson = """
                {"messages":[{"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ",
                  "content":{
                    "main_items":[
                      {"type":"TEXT","text":"Or pay in 4 interest-free payments with "},
                      {"type":"IMAGE","name":"paypal_logo","alternative_text":"PayPal",
                       "source_url":"https://paypal.com/logo.png"},
                      {"type":"TEXT","text":"."}
                    ],
                    "disclaimer_items":[{"type":"TEXT","text":"Available to US residents only."}],
                    "action_items":[{"type":"LINK","text":"Learn more","click_url":"https://paypal.com/learn"}]
                  }},
                  "selection_reasons":[{"code":"DEFAULT_PREFERRED","description":"default"}]}]}
            """.trimIndent()
            val braintreeClient = MockkBraintreeClientBuilder()
                .configurationSuccess(mockConfiguration())
                .build()
            coEvery { braintreeClient.sendPOST(url = any(), data = any()) } returns responseJson

            val sut = PayPalSavedPaymentMethodClient(braintreeClient, payPalClient)
            val request = PayPalCreditMessagingRequest.forAmount(currencyCode = "USD", value = "55.00")

            val result = sut.fetchCreditPresentmentMessages(request)

            requireNotNull(result)
            assertEquals(
                "Or pay in 4 interest-free payments with PayPal. Available to US residents only.",
                result.message
            )
            assertEquals("Learn more", result.learnMoreText)
            assertEquals("https://paypal.com/learn", result.learnMoreUrl)
        }

    @Test
    fun fetchCreditPresentmentMessages_whenEnvironmentIsNotProduction_postsToSandboxUrl() =
        runTest(testDispatcher) {
            val responseJson = """{"messages":[{"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ"}}]}"""
            val urlSlot = slot<String>()
            val braintreeClient = MockkBraintreeClientBuilder()
                .configurationSuccess(mockConfiguration(env = "sandbox"))
                .build()
            coEvery {
                braintreeClient.sendPOST(url = capture(urlSlot), data = any())
            } returns responseJson

            val sut = PayPalSavedPaymentMethodClient(braintreeClient, payPalClient)
            val request = PayPalCreditMessagingRequest.forAmount(currencyCode = "USD", value = "55.00")

            sut.fetchCreditPresentmentMessages(request)

            assertEquals(
                "https://api.sandbox.paypal.com/v2/credit/fetch-presentment-messages",
                urlSlot.captured
            )
        }

    @Test
    fun fetchCreditPresentmentMessages_whenNoPreferredMessage_returnsNull() = runTest(testDispatcher) {
        val responseJson = """{"messages":[{}]}"""
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(mockConfiguration())
            .build()
        coEvery { braintreeClient.sendPOST(url = any(), data = any()) } returns responseJson

        val sut = PayPalSavedPaymentMethodClient(braintreeClient, payPalClient)
        val request = PayPalCreditMessagingRequest.forAmount(currencyCode = "USD", value = "55.00")

        val result = sut.fetchCreditPresentmentMessages(request)

        assertNull(result)
    }

    @Test
    fun fetchCreditPresentmentMessages_withCallback_deliversResultAsynchronously() =
        runTest(testDispatcher) {
            val responseJson = """
                {"messages":[{"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ",
                  "content":{"main_items":[{"type":"TEXT","text":"As low as \${'$'}10/mo"}]}}}]}
            """.trimIndent()
            val braintreeClient = MockkBraintreeClientBuilder()
                .configurationSuccess(mockConfiguration())
                .build()
            coEvery { braintreeClient.sendPOST(url = any(), data = any()) } returns responseJson

            val sut = PayPalSavedPaymentMethodClient(
                braintreeClient,
                payPalClient,
                coroutineScope = CoroutineScope(testDispatcher)
            )
            val request = PayPalCreditMessagingRequest.forAmount(currencyCode = "USD", value = "55.00")

            var result: PayPalCreditMessagingContent? = null
            sut.fetchCreditPresentmentMessages(request) { result = it }
            testDispatcher.scheduler.advanceUntilIdle()

            requireNotNull(result)
            assertEquals("As low as \$10/mo", result?.message)
        }

    @Test
    fun fetchCreditPresentmentMessages_whenNetworkError_returnsNull() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(mockConfiguration())
            .build()
        coEvery { braintreeClient.sendPOST(url = any(), data = any()) } throws IOException("network down")

        val sut = PayPalSavedPaymentMethodClient(braintreeClient, payPalClient)
        val request = PayPalCreditMessagingRequest.forAmount(currencyCode = "USD", value = "55.00")

        val result = sut.fetchCreditPresentmentMessages(request)

        assertNull(result)
    }
}
