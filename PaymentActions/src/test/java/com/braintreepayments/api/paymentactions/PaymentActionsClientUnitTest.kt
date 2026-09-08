package com.braintreepayments.api.paymentactions

import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PaymentActionsClientUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val service = mockk<PaymentActionsService>()
    private val braintreeClient = mockk<BraintreeClient>(relaxed = true)

    private fun paymentAction(status: PaymentActionStatus) = PaymentAction(id = "pa123", status = status)

    private fun buildClient() = PaymentActionsClient(
        braintreeClient,
        service,
        testDispatcher,
        CoroutineScope(testDispatcher),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with REQUIRES_PAYMENT_METHOD returns PaymentMethodRequired`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.REQUIRES_PAYMENT_METHOD)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.PaymentMethodRequired>(result)
        assertEquals("pa123", outcome.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with REQUIRES_CUSTOMER_ACTION returns CustomerActionRequired`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.REQUIRES_CUSTOMER_ACTION)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.CustomerActionRequired>(result)
        assertEquals("pa123", outcome.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with READY_FOR_CONFIRMATION returns ServerActionRequired CONFIRM`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.READY_FOR_CONFIRMATION)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
        assertEquals("pa123", outcome.id)
        assertEquals(ServerAction.CONFIRM, outcome.serverAction)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with REQUIRES_CAPTURE returns ServerActionRequired CAPTURE`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.REQUIRES_CAPTURE)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
        assertEquals("pa123", outcome.id)
        assertEquals(ServerAction.CAPTURE, outcome.serverAction)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with SUCCEEDED returns Completed`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.SUCCEEDED)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.Completed>(result)
        assertEquals("pa123", outcome.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with CANCELLED returns Canceled`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.CANCELLED)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.Canceled>(result)
        assertEquals("pa123", outcome.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with EXPIRED returns Expired`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.EXPIRED)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.Expired>(result)
        assertEquals("pa123", outcome.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with PROCESSING returns Processing`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.PROCESSING)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.Processing>(result)
        assertEquals("pa123", outcome.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with UNKNOWN returns Unknown carrying the id`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.UNKNOWN)
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.Unknown>(result)
        assertEquals("pa123", outcome.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction propagates service Failure`() = runTest(testDispatcher) {
        val error = IOException("Network error")
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Failure(error)

        val result = buildClient().handleNextAction()

        val failure = assertIs<PaymentActionResult.Failure>(result)
        assertEquals(error, failure.error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `submitForPaymentAction with SUCCEEDED returns Completed`() = runTest(testDispatcher) {
        val creditCard = CreditCard(number = "4111111111111111", expirationMonth = "12", expirationYear = "2028")
        coEvery { service.setPaymentActionPaymentMethod(creditCard) } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.SUCCEEDED)
        )

        val result = buildClient().submitForPaymentAction(creditCard)

        val outcome = assertIs<PaymentActionResult.Completed>(result)
        assertEquals("pa123", outcome.id)
        verify { braintreeClient.sendAnalyticsEvent(PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_STARTED) }
        verify {
            braintreeClient.sendAnalyticsEvent(PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_SUCCEEDED)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `submitForPaymentAction propagates service Failure`() = runTest(testDispatcher) {
        val creditCard = CreditCard(number = "4111111111111111", expirationMonth = "12", expirationYear = "2028")
        val error = IOException("Network error")
        coEvery { service.setPaymentActionPaymentMethod(creditCard) } returns PaymentActionServiceResult.Failure(error)

        val result = buildClient().submitForPaymentAction(creditCard)

        val failure = assertIs<PaymentActionResult.Failure>(result)
        assertEquals(error, failure.error)
        verify { braintreeClient.sendAnalyticsEvent(PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_STARTED) }
        verify {
            braintreeClient.sendAnalyticsEvent(
                PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_FAILED,
                AnalyticsEventParams(errorDescription = error.message),
                any(),
            )
        }
    }
}
