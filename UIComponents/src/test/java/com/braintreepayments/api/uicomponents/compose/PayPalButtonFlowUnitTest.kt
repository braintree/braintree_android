package com.braintreepayments.api.uicomponents.compose

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PayPalButtonFlowUnitTest {

    private val payPalLauncher = mockk<PayPalLauncher>()
    private val payPalClient = mockk<PayPalClient>()
    private val pendingRequestRepository = mockk<PendingRequestRepository>()
    private val paymentAuthRequest = mockk<PayPalPaymentAuthRequest.ReadyToLaunch>()
    private val intent = mockk<Intent>()

    private val results = mutableListOf<PayPalResult>()
    private val callback = PayPalTokenizeCallback { results.add(it) }

    @Before
    fun setUp() {
        coEvery { pendingRequestRepository.storePendingRequest(any()) } just runs
    }

    // region completePayPalFlow

    @Test
    fun `completePayPalFlow returns false and reports a failure when the activity is not a ComponentActivity`() =
        runTest {
            val launched = completePayPalFlow(
                payPalLauncher,
                pendingRequestRepository,
                mockk<Activity>(),
                paymentAuthRequest,
                callback
            )

            assertFalse(launched)
            assertTrue(results.single() is PayPalResult.Failure)
            verify(exactly = 0) { payPalLauncher.launch(any(), any()) }
            coVerify(exactly = 0) { pendingRequestRepository.storePendingRequest(any()) }
        }

    @Test
    fun `completePayPalFlow returns false and reports the error when the launch fails`() = runTest {
        val error = Exception("launch failed")
        every { payPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Failure(error)

        val launched = completePayPalFlow(
            payPalLauncher,
            pendingRequestRepository,
            mockk<ComponentActivity>(),
            paymentAuthRequest,
            callback
        )

        assertFalse(launched)
        assertSame(error, (results.single() as PayPalResult.Failure).error)
        coVerify(exactly = 0) { pendingRequestRepository.storePendingRequest(any()) }
    }

    @Test
    fun `completePayPalFlow stores the pending request and returns true when the launch starts`() = runTest {
        every { payPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Started("pending-request")

        val launched = completePayPalFlow(
            payPalLauncher,
            pendingRequestRepository,
            mockk<ComponentActivity>(),
            paymentAuthRequest,
            callback
        )

        assertTrue(launched)
        assertTrue(results.isEmpty())
        coVerify(exactly = 1) { pendingRequestRepository.storePendingRequest("pending-request") }
    }

    // endregion

    // region handleReturnToApp

    @Test
    fun `handleReturnToApp reports a PendingRequestException when there is no pending request`() {
        handleReturnToApp(payPalLauncher, payPalClient, "", intent, callback)

        val failure = results.single() as PayPalResult.Failure
        assertTrue(failure.error is PendingRequestException)
        verify(exactly = 0) { payPalLauncher.handleReturnToApp(any(), any()) }
    }

    @Test
    fun `handleReturnToApp reports cancel when there is no result`() {
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult

        handleReturnToApp(payPalLauncher, payPalClient, "pending-request", intent, callback)

        assertEquals(PayPalResult.Cancel, results.single())
    }

    @Test
    fun `handleReturnToApp reports the error when the return fails`() {
        val error = Exception("return failed")
        val authResult = mockk<PayPalPaymentAuthResult.Failure>()
        every { authResult.error } returns error
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns authResult

        handleReturnToApp(payPalLauncher, payPalClient, "pending-request", intent, callback)

        assertSame(error, (results.single() as PayPalResult.Failure).error)
    }

    @Test
    fun `handleReturnToApp tokenizes a successful return and reports the tokenize result`() {
        val tokenizeError = Exception("tokenize failed")
        val authResult = mockk<PayPalPaymentAuthResult.Success>()
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns authResult
        every { payPalClient.tokenize(authResult, any()) } answers {
            secondArg<PayPalTokenizeCallback>().onPayPalResult(PayPalResult.Failure(tokenizeError))
        }

        handleReturnToApp(payPalLauncher, payPalClient, "pending-request", intent, callback)

        assertSame(tokenizeError, (results.single() as PayPalResult.Failure).error)
        verify(exactly = 1) { payPalClient.tokenize(authResult, any()) }
    }

    // endregion
}
