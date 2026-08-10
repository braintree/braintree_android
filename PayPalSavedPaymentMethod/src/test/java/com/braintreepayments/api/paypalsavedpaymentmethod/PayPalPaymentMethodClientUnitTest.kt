package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalBetaApi::class)
@RunWith(RobolectricTestRunner::class)
class PayPalPaymentMethodClientUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val payPalClient = mockk<PayPalClient>(relaxed = true)

    @Test
    fun fetchFI_withJwt_postsStickyFiBodyAndReturnsSuccess() = runTest(testDispatcher) {
        val responseJson = """{"data":{"getSavedPaymentMethod":{"payer":null,"paymentMethods":[]}}}"""
        val bodySlot = slot<JSONObject>()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(capture(bodySlot)) } returns responseJson

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("pmid-jwt")

        val input = bodySlot.captured.getJSONObject("variables").getJSONObject("input")
        assertEquals("STICKY_FI", input.getString("fetchPaymentMethodType"))
        assertEquals("pmid-jwt", input.getString("paymentMethodIdJwt"))
        assertTrue(result is PayPalPaymentMethodSummaryResult.Success)
    }

    @Test
    fun fetchFI_whenJwtMissing_returnsFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("")

        assertTrue(result is PayPalPaymentMethodSummaryResult.Failure)
        val error = (result as PayPalPaymentMethodSummaryResult.Failure).error
        assertTrue(error is PayPalPaymentMethodSummaryException)
        assertEquals(
            PayPalPaymentMethodSummaryException.MISSING_PAYMENT_METHOD_ID_JWT,
            error.message
        )
    }

    @Test
    fun refetchFI_postsApprovedCheckoutBodyAndReturnsSuccess() = runTest(testDispatcher) {
        val responseJson = """{"data":{"getSavedPaymentMethod":{"payer":null,"paymentMethods":[]}}}"""
        val bodySlot = slot<JSONObject>()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(capture(bodySlot)) } returns responseJson

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.refetchFI("order-123")

        val input = bodySlot.captured.getJSONObject("variables").getJSONObject("input")
        assertEquals("FI_FROM_APPROVED_CHECKOUT", input.getString("fetchPaymentMethodType"))
        assertEquals("order-123", input.getString("orderId"))
        assertTrue(result is PayPalPaymentMethodSummaryResult.Success)
    }

    @Test
    fun fetchFI_whenGraphQLReturnsErrors_returnsFailurePreservingErrorClass() = runTest(testDispatcher) {
        val responseJson = """
            {"errors":[{"message":"PayPal access token not found for merchant account.",
              "extensions":{"errorClass":"AUTHENTICATION","errorType":"developer_error"}}],
             "data":{"getSavedPaymentMethod":null}}
        """.trimIndent()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(any()) } returns responseJson

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("pmid-jwt")

        assertTrue(result is PayPalPaymentMethodSummaryResult.Failure)
        val error = (result as PayPalPaymentMethodSummaryResult.Failure).error
        assertTrue(error is PayPalPaymentMethodSummaryException)
        assertEquals("AUTHENTICATION", (error as PayPalPaymentMethodSummaryException).errorClass)
    }

    @Test
    fun fetchFI_whenNetworkError_returnsFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(any()) } throws IOException("network down")

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("pmid-jwt")

        assertTrue(result is PayPalPaymentMethodSummaryResult.Failure)
        assertTrue((result as PayPalPaymentMethodSummaryResult.Failure).error is IOException)
    }
}
