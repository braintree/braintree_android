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
    private val threeDSecureProvider = mockk<ThreeDSecureProvider>()
    private val sut = NextActionHandler(service, threeDSecureProvider)

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

            NextActionHandler(service, threeDSecureProvider).handle()

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
    fun `handle with REQUIRES_CUSTOMER_ACTION stops at the boundary for every nextAction it can't resolve`() =
        runTest(testDispatcher) {
            val nextActions = listOf(
                NextAction.Unrecognized(typeName = "PaymentActionRedirectUrl"),
                null,
            )
            nextActions.forEach { nextAction ->
                val service = mockk<PaymentActionsService>()
                coEvery { service.getPaymentAction() } returns
                    success(PaymentActionStatus.REQUIRES_CUSTOMER_ACTION, nextAction = nextAction)

                val result = NextActionHandler(service, threeDSecureProvider).handle()

                assertEquals("pa123", assertIs<PaymentActionResult.CustomerActionRequired>(result).id)
                coVerify(exactly = 1) { service.getPaymentAction() }
                coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
            }
            coVerify(exactly = 0) { threeDSecureProvider.collectDeviceData() }
            coVerify(exactly = 0) { threeDSecureProvider.presentChallenge(any(), any()) }
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

    private val authentication = NextAction.ThreeDSecureAuthentication(bin = "401288")
    private val ddcSignal = CustomerCompletedAction.DeviceDataCollection(dfReferenceId = "df-ref")

    private fun requiresDeviceData(version: Int? = null) =
        success(PaymentActionStatus.REQUIRES_CUSTOMER_ACTION, version = version, nextAction = authentication)

    private fun stubDeviceDataCollected() {
        coEvery { threeDSecureProvider.collectDeviceData() } returns DeviceDataResult.Collected("df-ref")
    }

    @Test
    fun `handle with a device data request collects, signals, and continues on the signal response`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns requiresDeviceData()
            stubDeviceDataCollected()
            coEvery { service.signalCustomerActionComplete(ddcSignal) } returns
                success(PaymentActionStatus.READY_FOR_CONFIRMATION)

            val result = sut.handle()

            val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
            assertEquals(ServerAction.CONFIRM, outcome.serverAction)
            coVerify(exactly = 1) { threeDSecureProvider.collectDeviceData() }
            coVerify(exactly = 1) { service.signalCustomerActionComplete(ddcSignal) }
            coVerify(exactly = 1) { service.getPaymentAction() }
        }

    @Test
    fun `handle with a device data request from the passed-in result collects without fetching`() =
        runTest(testDispatcher) {
            stubDeviceDataCollected()
            coEvery { service.signalCustomerActionComplete(ddcSignal) } returns success(PaymentActionStatus.SUCCEEDED)

            val result = sut.handle(requiresDeviceData())

            assertIs<PaymentActionResult.Completed>(result)
            coVerify(exactly = 0) { service.getPaymentAction() }
        }

    @Test
    fun `handle with a PROCESSING signal response polls until the Payment Action resolves`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returnsMany listOf(
                requiresDeviceData(),
                success(PaymentActionStatus.SUCCEEDED),
            )
            stubDeviceDataCollected()
            coEvery { service.signalCustomerActionComplete(ddcSignal) } returns success(PaymentActionStatus.PROCESSING)

            val result = sut.handle()

            assertIs<PaymentActionResult.Completed>(result)
            coVerify(exactly = 2) { service.getPaymentAction() }
            assertEquals(NextActionHandler.PROCESSING_RETRY_DELAY.inWholeMilliseconds, currentTime)
        }

    @Test
    fun `handle with a challenge in the device data signal response presents it and continues`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns requiresDeviceData()
            stubDeviceDataCollected()
            coEvery { service.signalCustomerActionComplete(ddcSignal) } returns requiresChallenge()
            stubChallengeCompleted()
            coEvery { service.signalCustomerActionComplete(challengeSignal) } returns
                success(PaymentActionStatus.SUCCEEDED)

            val result = sut.handle()

            assertEquals("pa123", assertIs<PaymentActionResult.Completed>(result).id)
            coVerify(exactly = 1) { threeDSecureProvider.presentChallenge("mpi-tx-id", "challenge-payload") }
            coVerify(exactly = 1) { service.getPaymentAction() }
        }

    @Test
    fun `handle returns Failure without signaling when device data collection fails`() =
        runTest(testDispatcher) {
            val error = BraintreeException("setup failed")
            coEvery { service.getPaymentAction() } returns requiresDeviceData()
            coEvery { threeDSecureProvider.collectDeviceData() } returns DeviceDataResult.Failed(error)

            val result = sut.handle()

            assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
            coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
        }

    @Test
    fun `handle returns Failure when the device data signal fails`() = runTest(testDispatcher) {
        val error = IOException("Network error")
        coEvery { service.getPaymentAction() } returns requiresDeviceData()
        stubDeviceDataCollected()
        coEvery { service.signalCustomerActionComplete(ddcSignal) } returns PaymentActionServiceResult.Failure(error)

        val result = sut.handle()

        assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
    }

    @Test
    fun `handle detects a version decrease in the device data signal response`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns requiresDeviceData(version = 2)
        stubDeviceDataCollected()
        coEvery { service.signalCustomerActionComplete(ddcSignal) } returns
            success(PaymentActionStatus.SUCCEEDED, version = 1)

        val result = sut.handle()

        val error = assertIs<BraintreeException>(assertIs<PaymentActionResult.Failure>(result).error)
        assertEquals(NextActionHandler.VERSION_ANOMALY_MESSAGE, error.message)
    }

    @Test
    fun `handle returns CustomerActionRequired when the signal response requests device data again`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns requiresDeviceData()
            stubDeviceDataCollected()
            coEvery { service.signalCustomerActionComplete(ddcSignal) } returns requiresDeviceData()

            val result = sut.handle()

            assertEquals("pa123", assertIs<PaymentActionResult.CustomerActionRequired>(result).id)
            coVerify(exactly = 1) { threeDSecureProvider.collectDeviceData() }
            coVerify(exactly = 1) { service.signalCustomerActionComplete(ddcSignal) }
        }

    @Test
    fun `handle collects device data at most once even when a later poll requests it again`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returnsMany listOf(
                requiresDeviceData(),
                requiresDeviceData(),
            )
            stubDeviceDataCollected()
            coEvery { service.signalCustomerActionComplete(ddcSignal) } returns success(PaymentActionStatus.PROCESSING)

            val result = sut.handle()

            assertIs<PaymentActionResult.CustomerActionRequired>(result)
            coVerify(exactly = 1) { threeDSecureProvider.collectDeviceData() }
            coVerify(exactly = 2) { service.getPaymentAction() }
        }

    private val challenge = NextAction.ThreeDSecureChallenge(
        acsUrl = "https://acs.example.com",
        acsRenderingType = null,
        challengePayload = "challenge-payload",
        mpiTransactionId = "mpi-tx-id",
    )
    private val challengeSignal = CustomerCompletedAction.Challenge(cardinalAuthenticationJWT = "server-jwt")

    private fun requiresChallenge(
        nextAction: NextAction.ThreeDSecureChallenge = challenge,
        version: Int? = null,
    ) = success(PaymentActionStatus.REQUIRES_CUSTOMER_ACTION, version = version, nextAction = nextAction)

    private fun stubChallengeCompleted() {
        coEvery { threeDSecureProvider.presentChallenge(any(), any()) } returns ChallengeResult.Completed("server-jwt")
    }

    @Test
    fun `handle with a challenge presents it, signals the JWT, and continues on the signal response`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns requiresChallenge()
            stubChallengeCompleted()
            coEvery { service.signalCustomerActionComplete(challengeSignal) } returns
                success(PaymentActionStatus.READY_FOR_CONFIRMATION)

            val result = sut.handle()

            val outcome = assertIs<PaymentActionResult.ServerActionRequired>(result)
            assertEquals(ServerAction.CONFIRM, outcome.serverAction)
            coVerify(exactly = 1) { threeDSecureProvider.presentChallenge("mpi-tx-id", "challenge-payload") }
            coVerify(exactly = 1) { service.signalCustomerActionComplete(challengeSignal) }
            coVerify(exactly = 1) { service.getPaymentAction() }
            coVerify(exactly = 0) { threeDSecureProvider.collectDeviceData() }
        }

    @Test
    fun `handle with a challenge from the passed-in result presents it without fetching`() =
        runTest(testDispatcher) {
            stubChallengeCompleted()
            coEvery { service.signalCustomerActionComplete(challengeSignal) } returns
                success(PaymentActionStatus.SUCCEEDED)

            val result = sut.handle(requiresChallenge())

            assertIs<PaymentActionResult.Completed>(result)
            coVerify(exactly = 0) { service.getPaymentAction() }
        }

    @Test
    fun `handle with a challenge missing its mpiTransactionId returns Failure without presenting it`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns
                requiresChallenge(challenge.copy(mpiTransactionId = null))

            val result = sut.handle()

            val error = assertIs<BraintreeException>(assertIs<PaymentActionResult.Failure>(result).error)
            assertEquals(NextActionHandler.MISSING_MPI_TRANSACTION_ID_MESSAGE, error.message)
            coVerify(exactly = 0) { threeDSecureProvider.presentChallenge(any(), any()) }
            coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
        }

    @Test
    fun `handle with an abandoned challenge returns ThreeDSecureCanceled without signaling`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns requiresChallenge()
            coEvery { threeDSecureProvider.presentChallenge(any(), any()) } returns ChallengeResult.Abandoned

            val result = sut.handle()

            assertEquals("pa123", assertIs<PaymentActionResult.ThreeDSecureCanceled>(result).id)
            coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
        }

    @Test
    fun `handle returns Failure without signaling when the challenge fails`() = runTest(testDispatcher) {
        val error = BraintreeException("challenge failed")
        coEvery { service.getPaymentAction() } returns requiresChallenge()
        coEvery { threeDSecureProvider.presentChallenge(any(), any()) } returns ChallengeResult.Failed(error)

        val result = sut.handle()

        assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
        coVerify(exactly = 0) { service.signalCustomerActionComplete(any()) }
    }

    @Test
    fun `handle returns Failure when the challenge signal fails`() = runTest(testDispatcher) {
        val error = IOException("Network error")
        coEvery { service.getPaymentAction() } returns requiresChallenge()
        stubChallengeCompleted()
        coEvery { service.signalCustomerActionComplete(challengeSignal) } returns
            PaymentActionServiceResult.Failure(error)

        val result = sut.handle()

        assertSame(error, assertIs<PaymentActionResult.Failure>(result).error)
    }

    @Test
    fun `handle detects a version decrease in the challenge signal response`() = runTest(testDispatcher) {
        coEvery { service.getPaymentAction() } returns requiresChallenge(version = 3)
        stubChallengeCompleted()
        coEvery { service.signalCustomerActionComplete(challengeSignal) } returns
            success(PaymentActionStatus.SUCCEEDED, version = 2)

        val result = sut.handle()

        val error = assertIs<BraintreeException>(assertIs<PaymentActionResult.Failure>(result).error)
        assertEquals(NextActionHandler.VERSION_ANOMALY_MESSAGE, error.message)
    }

    @Test
    fun `handle returns CustomerActionRequired when the signal response requests the challenge again`() =
        runTest(testDispatcher) {
            coEvery { service.getPaymentAction() } returns requiresChallenge()
            stubChallengeCompleted()
            coEvery { service.signalCustomerActionComplete(challengeSignal) } returns requiresChallenge()

            val result = sut.handle()

            assertEquals("pa123", assertIs<PaymentActionResult.CustomerActionRequired>(result).id)
            coVerify(exactly = 1) { threeDSecureProvider.presentChallenge(any(), any()) }
            coVerify(exactly = 1) { service.signalCustomerActionComplete(challengeSignal) }
        }
}
