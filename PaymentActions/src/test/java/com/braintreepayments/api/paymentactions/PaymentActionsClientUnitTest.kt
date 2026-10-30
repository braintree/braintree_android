package com.braintreepayments.api.paymentactions

import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

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
    fun `handleNextAction with PROCESSING that never resolves returns Processing after max re-checks`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
                paymentAction(PaymentActionStatus.PROCESSING)
            )

            val result = buildClient().handleNextAction()

            val outcome = assertIs<PaymentActionResult.Processing>(result)
            assertEquals("pa123", outcome.id)
            coVerify(exactly = NextActionHandler.MAX_PROCESSING_ATTEMPTS) { service.getPaymentAction() }
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
    fun `handleNextAction with PROCESSING then SUCCEEDED returns Completed`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            PaymentActionServiceResult.Success(paymentAction(PaymentActionStatus.PROCESSING)),
            PaymentActionServiceResult.Success(paymentAction(PaymentActionStatus.SUCCEEDED)),
        )

        val result = buildClient().handleNextAction()

        val outcome = assertIs<PaymentActionResult.Completed>(result)
        assertEquals("pa123", outcome.id)
        coVerify(exactly = 2) { service.getPaymentAction() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with REQUIRES_CUSTOMER_ACTION and a 3DS nextAction returns CustomerActionRequired without driving 3DS`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
                PaymentAction(
                    id = "pa123",
                    status = PaymentActionStatus.REQUIRES_CUSTOMER_ACTION,
                    nextAction = NextAction.ThreeDSecureAuthentication(bin = "401288"),
                )
            )

            val result = buildClient().handleNextAction()

            assertIs<PaymentActionResult.CustomerActionRequired>(result)
            coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction wraps an unexpected throwable in Failure`() = runTest(testDispatcher) {
        val error = IllegalStateException("unexpected")
        coEvery { service.getPaymentAction() } throws error

        val result = buildClient().handleNextAction()

        val failure = assertIs<PaymentActionResult.Failure>(result)
        assertSame(error, failure.error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction rethrows CancellationException instead of wrapping it`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } throws CancellationException("cancelled")

        assertFailsWith<CancellationException> { buildClient().handleNextAction() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction called while a previous call is in flight returns Failure`() = runTest(testDispatcher) {
        val firstFetch = CompletableDeferred<PaymentActionServiceResult>()
        coEvery { service.getPaymentAction() } coAnswers { firstFetch.await() }
        val client = buildClient()

        val first = async { client.handleNextAction() }
        runCurrent() // first call is now suspended inside the loop, holding the guard

        val second = client.handleNextAction()

        val failure = assertIs<PaymentActionResult.Failure>(second)
        val error = assertIs<BraintreeException>(failure.error)
        assertEquals(PaymentActionsClient.HANDLE_NEXT_ACTION_IN_PROGRESS_MESSAGE, error.message)

        firstFetch.complete(PaymentActionServiceResult.Success(paymentAction(PaymentActionStatus.SUCCEEDED)))
        assertIs<PaymentActionResult.Completed>(first.await())
        coVerify(exactly = 1) { service.getPaymentAction() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction can be called again after a previous call completes`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.SUCCEEDED)
        )
        val client = buildClient()

        assertIs<PaymentActionResult.Completed>(client.handleNextAction())
        assertIs<PaymentActionResult.Completed>(client.handleNextAction())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction can be called again after a previous call fails`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } throws IllegalStateException("unexpected") andThen
            PaymentActionServiceResult.Success(paymentAction(PaymentActionStatus.SUCCEEDED))
        val client = buildClient()

        assertIs<PaymentActionResult.Failure>(client.handleNextAction())
        assertIs<PaymentActionResult.Completed>(client.handleNextAction())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleNextAction with callback delivers the result`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.SUCCEEDED)
        )
        var result: PaymentActionResult? = null

        buildClient().handleNextAction { result = it }
        advanceUntilIdle()

        assertIs<PaymentActionResult.Completed>(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `submitForPaymentAction with PROCESSING returns Processing without re-checking`() = runTest(testDispatcher) {
        val creditCard = CreditCard(number = "4111111111111111", expirationMonth = "12", expirationYear = "2028")
        coEvery { service.setPaymentActionPaymentMethod(creditCard) } returns PaymentActionServiceResult.Success(
            paymentAction(PaymentActionStatus.PROCESSING)
        )

        val result = buildClient().submitForPaymentAction(creditCard)

        assertIs<PaymentActionResult.Processing>(result)
        coVerify(exactly = 1) { service.setPaymentActionPaymentMethod(creditCard) }
        coVerify(exactly = 0) { service.getPaymentAction() }
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
