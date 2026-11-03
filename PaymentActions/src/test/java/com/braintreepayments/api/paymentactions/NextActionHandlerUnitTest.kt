package com.braintreepayments.api.paymentactions

import com.braintreepayments.api.core.BraintreeException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class NextActionHandlerUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val service = mockk<PaymentActionsService>()
    private val sut = NextActionHandler(service)

    private fun success(
        status: PaymentActionStatus,
        version: Int? = null,
        nextAction: NextAction? = null,
    ) = PaymentActionServiceResult.Success(
        PaymentAction(id = "pa123", status = status, version = version, nextAction = nextAction)
    )

    @Test
    fun `toPaymentActionResult with REQUIRES_PAYMENT_METHOD returns PaymentMethodRequired`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.REQUIRES_PAYMENT_METHOD))

        assertEquals("pa123", assertIs<PaymentActionResult.PaymentMethodRequired>(result).id)
    }

    @Test
    fun `toPaymentActionResult with REQUIRES_CUSTOMER_ACTION returns CustomerActionRequired`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.REQUIRES_CUSTOMER_ACTION))

        assertEquals("pa123", assertIs<PaymentActionResult.CustomerActionRequired>(result).id)
    }

    @Test
    fun `toPaymentActionResult with READY_FOR_CONFIRMATION returns ServerActionRequired CONFIRM`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.READY_FOR_CONFIRMATION))

        val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
        assertEquals("pa123", outcome.id)
        assertEquals(ServerAction.CONFIRM, outcome.serverAction)
    }

    @Test
    fun `toPaymentActionResult with REQUIRES_CAPTURE returns ServerActionRequired CAPTURE`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.REQUIRES_CAPTURE))

        val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
        assertEquals("pa123", outcome.id)
        assertEquals(ServerAction.CAPTURE, outcome.serverAction)
    }

    @Test
    fun `toPaymentActionResult with SUCCEEDED returns Completed`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.SUCCEEDED))

        assertEquals("pa123", assertIs<PaymentActionResult.Completed>(result).id)
    }

    @Test
    fun `toPaymentActionResult with CANCELLED returns Canceled`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.CANCELLED))

        assertEquals("pa123", assertIs<PaymentActionResult.Canceled>(result).id)
    }

    @Test
    fun `toPaymentActionResult with EXPIRED returns Expired`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.EXPIRED))

        assertEquals("pa123", assertIs<PaymentActionResult.Expired>(result).id)
    }

    @Test
    fun `toPaymentActionResult with PROCESSING returns Processing`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.PROCESSING))

        assertEquals("pa123", assertIs<PaymentActionResult.Processing>(result).id)
    }

    @Test
    fun `toPaymentActionResult with UNKNOWN returns Unrecognized`() {
        val result = sut.toPaymentActionResult(success(PaymentActionStatus.UNKNOWN))

        assertEquals("pa123", assertIs<PaymentActionResult.Unrecognized>(result).id)
    }

    @Test
    fun `toPaymentActionResult with a service Failure returns Failure with the same error`() {
        val error = IOException("Network error")

        val result = sut.toPaymentActionResult(PaymentActionServiceResult.Failure(error))

        assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
    }

    @Test
    fun `handle with a non-PROCESSING status returns its mapped result after one fetch`() = runTest(testDispatcher) {
        val nonProcessingStatuses = PaymentActionStatus.entries - PaymentActionStatus.PROCESSING
        nonProcessingStatuses.forEach { status ->
            val service = mockk<PaymentActionsService>()
            val serviceResult = success(status)
            coEvery { service.getPaymentAction() } returns serviceResult
            val handler = NextActionHandler(service)

            val result = handler.handle()

            assertEquals(handler.toPaymentActionResult(serviceResult)::class, result::class)
            coVerify(exactly = 1) { service.getPaymentAction() }
        }
        assertEquals(0, currentTime)
    }

    @Test
    fun `handle with PROCESSING then SUCCEEDED re-fetches and returns Completed`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING),
            success(PaymentActionStatus.SUCCEEDED),
        )

        val result = sut.handle()

        assertEquals("pa123", assertIs<PaymentActionResult.Completed>(result).id)
        coVerify(exactly = 2) { service.getPaymentAction() }
        assertEquals(NextActionHandler.PROCESSING_RETRY_DELAY.inWholeMilliseconds, currentTime)
    }

    @Test
    fun `handle with PROCESSING that never resolves returns Processing after the max attempts`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns success(PaymentActionStatus.PROCESSING)

            val result = sut.handle()

            assertEquals("pa123", assertIs<PaymentActionResult.Processing>(result).id)
            coVerify(exactly = NextActionHandler.MAX_PROCESSING_ATTEMPTS) { service.getPaymentAction() }
            // One delay between each pair of fetches, none after the last.
            val expectedDelay =
                (NextActionHandler.MAX_PROCESSING_ATTEMPTS - 1) * NextActionHandler.PROCESSING_RETRY_DELAY.inWholeMilliseconds
            assertEquals(expectedDelay, currentTime)
        }

    @Test
    fun `handle with an increasing version across fetches does not fail`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 1),
            success(PaymentActionStatus.SUCCEEDED, version = 2),
        )

        val result = sut.handle()

        assertIs<PaymentActionResult.Completed>(result)
    }

    @Test
    fun `handle with an unchanged version across PROCESSING fetches does not fail`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 3),
            success(PaymentActionStatus.PROCESSING, version = 3),
            success(PaymentActionStatus.SUCCEEDED, version = 3),
        )

        val result = sut.handle()

        assertIs<PaymentActionResult.Completed>(result)
    }

    @Test
    fun `handle with a missing version does not fail`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 2),
            success(PaymentActionStatus.SUCCEEDED, version = null),
        )

        val result = sut.handle()

        assertIs<PaymentActionResult.Completed>(result)
    }

    @Test
    fun `handle with a decreasing version returns Failure with a BraintreeException`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 2),
            success(PaymentActionStatus.SUCCEEDED, version = 1),
        )

        val result = sut.handle()

        val failure = assertIs<PaymentActionResult.Failure>(result)
        val error = assertIs<BraintreeException>(failure.error)
        assertEquals(NextActionHandler.VERSION_ANOMALY_MESSAGE, error.message)
    }

    @Test
    fun `handle compares against the last known version when an intermediate version is missing`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returnsMany listOf(
                success(PaymentActionStatus.PROCESSING, version = 2),
                success(PaymentActionStatus.PROCESSING, version = null),
                success(PaymentActionStatus.SUCCEEDED, version = 1),
            )

            val result = sut.handle()

            assertIs<PaymentActionResult.Failure>(result)
        }

    @Test
    fun `handle with REQUIRES_CUSTOMER_ACTION stops at the boundary for every nextAction type`() =
        runTest(testDispatcher) {
            val nextActions = listOf(
                NextAction.ThreeDSecureAuthentication(bin = "401288"),
                NextAction.ThreeDSecureChallenge(
                    acsUrl = "https://acs.example.com",
                    acsRenderingType = null,
                    challengePayload = "payload",
                ),
                NextAction.Unrecognized(typeName = "PaymentActionRedirectUrl"),
                null,
            )
            nextActions.forEach { nextAction ->
                val service = mockk<PaymentActionsService>()
                coEvery { service.getPaymentAction() } returns
                    success(PaymentActionStatus.REQUIRES_CUSTOMER_ACTION, nextAction = nextAction)

                val result = NextActionHandler(service).handle()

                assertEquals("pa123", assertIs<PaymentActionResult.CustomerActionRequired>(result).id)
                coVerify(exactly = 1) { service.getPaymentAction() }
                coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
            }
        }

    @Test
    fun `handle returns a service Failure as a Failure result without retrying`() = runTest(testDispatcher) {
        val error = IOException("Network error")
        coEvery { service.getPaymentAction() } returns PaymentActionServiceResult.Failure(error)

        val result = sut.handle()

        assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
        coVerify(exactly = 1) { service.getPaymentAction() }
    }

    @Test
    fun `handle returns a Failure that occurs while re-checking PROCESSING`() = runTest(testDispatcher) {
        val error = IOException("Network error")
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING),
            PaymentActionServiceResult.Failure(error),
        )

        val result = sut.handle()

        assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
        coVerify(exactly = 2) { service.getPaymentAction() }
    }
}
