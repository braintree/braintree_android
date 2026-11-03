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

    /** Runs [NextActionHandler.handle] with a single fetch that returns [status]. */
    private suspend fun handleFetched(status: PaymentActionStatus): PaymentActionResult {
        coEvery { service.getPaymentAction() } returns success(status)
        return sut.handle()
    }

    @Test
    fun `handle with REQUIRES_PAYMENT_METHOD returns PaymentMethodRequired`() = runTest(testDispatcher) {
        val result = handleFetched(PaymentActionStatus.REQUIRES_PAYMENT_METHOD)

        assertEquals("pa123", assertIs<PaymentActionResult.PaymentMethodRequired>(result).id)
    }

    @Test
    fun `handle with READY_FOR_CONFIRMATION returns ServerActionRequired CONFIRM`() = runTest(testDispatcher) {
        val result = handleFetched(PaymentActionStatus.READY_FOR_CONFIRMATION)

        val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
        assertEquals("pa123", outcome.id)
        assertEquals(ServerAction.CONFIRM, outcome.serverAction)
    }

    @Test
    fun `handle with REQUIRES_CAPTURE returns ServerActionRequired CAPTURE`() = runTest(testDispatcher) {
        val result = handleFetched(PaymentActionStatus.REQUIRES_CAPTURE)

        val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
        assertEquals("pa123", outcome.id)
        assertEquals(ServerAction.CAPTURE, outcome.serverAction)
    }

    @Test
    fun `handle with SUCCEEDED returns Completed`() = runTest(testDispatcher) {
        val result = handleFetched(PaymentActionStatus.SUCCEEDED)

        assertEquals("pa123", assertIs<PaymentActionResult.Completed>(result).id)
    }

    @Test
    fun `handle with CANCELLED returns Canceled`() = runTest(testDispatcher) {
        val result = handleFetched(PaymentActionStatus.CANCELLED)

        assertEquals("pa123", assertIs<PaymentActionResult.Canceled>(result).id)
    }

    @Test
    fun `handle with EXPIRED returns Expired`() = runTest(testDispatcher) {
        val result = handleFetched(PaymentActionStatus.EXPIRED)

        assertEquals("pa123", assertIs<PaymentActionResult.Expired>(result).id)
    }

    @Test
    fun `handle with UNKNOWN returns Unrecognized`() = runTest(testDispatcher) {
        val result = handleFetched(PaymentActionStatus.UNKNOWN)

        assertEquals("pa123", assertIs<PaymentActionResult.Unrecognized>(result).id)
    }

    @Test
    fun `handle with a non-PROCESSING status stops after one fetch without delaying`() = runTest(testDispatcher) {
        val nonProcessingStatuses = PaymentActionStatus.entries - PaymentActionStatus.PROCESSING
        nonProcessingStatuses.forEach { status ->
            val service = mockk<PaymentActionsService>()
            coEvery { service.getPaymentAction() } returns success(status)

            NextActionHandler(service).handle()

            coVerify(exactly = 1) { service.getPaymentAction() }
        }
        assertEquals(0, currentTime)
    }

    @Test
    fun `handle with a passed-in non-PROCESSING result maps it without fetching`() = runTest(testDispatcher) {
        val result = sut.handle(success(PaymentActionStatus.SUCCEEDED))

        assertEquals("pa123", assertIs<PaymentActionResult.Completed>(result).id)
        coVerify(exactly = 0) { service.getPaymentAction() }
    }

    @Test
    fun `handle with a passed-in Failure returns it without fetching`() = runTest(testDispatcher) {
        val error = IOException("Network error")

        val result = sut.handle(PaymentActionServiceResult.Failure(error))

        assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
        coVerify(exactly = 0) { service.getPaymentAction() }
    }

    @Test
    fun `handle with a passed-in PROCESSING result fetches until the Payment Action resolves`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns success(PaymentActionStatus.SUCCEEDED)

            val result = sut.handle(success(PaymentActionStatus.PROCESSING))

            assertIs<PaymentActionResult.Completed>(result)
            coVerify(exactly = 1) { service.getPaymentAction() }
            assertEquals(NextActionHandler.PROCESSING_RETRY_DELAY.inWholeMilliseconds, currentTime)
        }

    @Test
    fun `handle with a passed-in PROCESSING result counts it as the first attempt`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns success(PaymentActionStatus.PROCESSING)

        val result = sut.handle(success(PaymentActionStatus.PROCESSING))

        assertIs<PaymentActionResult.Processing>(result)
        coVerify(exactly = NextActionHandler.MAX_PROCESSING_ATTEMPTS - 1) { service.getPaymentAction() }
    }

    @Test
    fun `handle detects a version decrease between the passed-in result and the first fetch`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns success(PaymentActionStatus.SUCCEEDED, version = 1)

            val result = sut.handle(success(PaymentActionStatus.PROCESSING, version = 2))

            val error = assertIs<BraintreeException>(assertIs<PaymentActionResult.Failure>(result).error)
            assertEquals(NextActionHandler.VERSION_ANOMALY_MESSAGE, error.message)
        }

    // endregion

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
