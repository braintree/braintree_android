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

    private fun PaymentActionServiceResult.status() =
        assertIs<PaymentActionServiceResult.Success>(this).paymentAction.status

    @Test
    fun `drive with a non-PROCESSING status returns immediately after one fetch`() = runTest(testDispatcher) {
        val nonProcessingStatuses = PaymentActionStatus.entries - PaymentActionStatus.PROCESSING
        nonProcessingStatuses.forEach { status ->
            val service = mockk<PaymentActionsService>()
            coEvery { service.getPaymentAction() } returns success(status)

            val result = NextActionHandler(service).drive()

            assertEquals(status, result.status())
            coVerify(exactly = 1) { service.getPaymentAction() }
        }
        assertEquals(0, currentTime)
    }

    @Test
    fun `drive with PROCESSING then SUCCEEDED re-fetches and returns the resolved status`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING),
            success(PaymentActionStatus.SUCCEEDED),
        )

        val result = sut.drive()

        assertEquals(PaymentActionStatus.SUCCEEDED, result.status())
        coVerify(exactly = 2) { service.getPaymentAction() }
        assertEquals(NextActionHandler.PROCESSING_RETRY_DELAY.inWholeMilliseconds, currentTime)
    }

    @Test
    fun `drive with PROCESSING that never resolves returns PROCESSING after the max attempts`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns success(PaymentActionStatus.PROCESSING)

            val result = sut.drive()

            assertEquals(PaymentActionStatus.PROCESSING, result.status())
            coVerify(exactly = NextActionHandler.MAX_PROCESSING_ATTEMPTS) { service.getPaymentAction() }
            // One delay between each pair of fetches, none after the last.
            val expectedDelay =
                (NextActionHandler.MAX_PROCESSING_ATTEMPTS - 1) * NextActionHandler.PROCESSING_RETRY_DELAY.inWholeMilliseconds
            assertEquals(expectedDelay, currentTime)
        }

    @Test
    fun `drive with an increasing version across fetches does not fail`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 1),
            success(PaymentActionStatus.SUCCEEDED, version = 2),
        )

        val result = sut.drive()

        assertEquals(PaymentActionStatus.SUCCEEDED, result.status())
    }

    @Test
    fun `drive with an unchanged version across PROCESSING fetches does not fail`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 3),
            success(PaymentActionStatus.PROCESSING, version = 3),
            success(PaymentActionStatus.SUCCEEDED, version = 3),
        )

        val result = sut.drive()

        assertEquals(PaymentActionStatus.SUCCEEDED, result.status())
    }

    @Test
    fun `drive with a missing version does not fail`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 2),
            success(PaymentActionStatus.SUCCEEDED, version = null),
        )

        val result = sut.drive()

        assertEquals(PaymentActionStatus.SUCCEEDED, result.status())
    }

    @Test
    fun `drive with a decreasing version returns Failure with a BraintreeException`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING, version = 2),
            success(PaymentActionStatus.SUCCEEDED, version = 1),
        )

        val result = sut.drive()

        val failure = assertIs<PaymentActionServiceResult.Failure>(result)
        val error = assertIs<BraintreeException>(failure.error)
        assertEquals(NextActionHandler.VERSION_ANOMALY_MESSAGE, error.message)
    }

    @Test
    fun `drive compares against the last known version when an intermediate version is missing`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returnsMany listOf(
                success(PaymentActionStatus.PROCESSING, version = 2),
                success(PaymentActionStatus.PROCESSING, version = null),
                success(PaymentActionStatus.SUCCEEDED, version = 1),
            )

            val result = sut.drive()

            assertIs<PaymentActionServiceResult.Failure>(result)
        }

    @Test
    fun `drive with REQUIRES_CUSTOMER_ACTION stops at the boundary for every nextAction type`() =
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

                val result = NextActionHandler(service).drive()

                val success = assertIs<PaymentActionServiceResult.Success>(result)
                assertEquals(PaymentActionStatus.REQUIRES_CUSTOMER_ACTION, success.paymentAction.status)
                assertEquals(nextAction, success.paymentAction.nextAction)
                coVerify(exactly = 1) { service.getPaymentAction() }
                coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
            }
        }

    @Test
    fun `drive returns a service Failure as-is without retrying`() = runTest(testDispatcher) {
        val failure = PaymentActionServiceResult.Failure(IOException("Network error"))
        coEvery { service.getPaymentAction() } returns failure

        val result = sut.drive()

        assertSame(failure, result)
        coVerify(exactly = 1) { service.getPaymentAction() }
    }

    @Test
    fun `drive returns a Failure that occurs while re-checking PROCESSING`() = runTest(testDispatcher) {
        val failure = PaymentActionServiceResult.Failure(IOException("Network error"))
        coEvery { service.getPaymentAction() } returnsMany listOf(
            success(PaymentActionStatus.PROCESSING),
            failure,
        )

        val result = sut.drive()

        assertSame(failure, result)
        coVerify(exactly = 2) { service.getPaymentAction() }
    }
}
