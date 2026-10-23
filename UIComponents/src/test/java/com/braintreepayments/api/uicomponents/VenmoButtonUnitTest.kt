package com.braintreepayments.api.uicomponents

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequestCallback
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VenmoButtonUnitTest {

    private val mockVenmoClient: VenmoClient = mockk(relaxed = true)
    private val mockVenmoLauncher: VenmoLauncher = mockk(relaxed = true)
    private val venmoRequest = VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE)

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        unmockkObject(AnalyticsClient.Companion)
    }

    @Test
    fun `setVenmoRequest when createPaymentAuthRequest returns Failure re-enables button`() {
        val callbackSlot = slot<VenmoPaymentAuthRequestCallback>()
        every {
            mockVenmoClient.createPaymentAuthRequest(any(), any(), capture(callbackSlot))
        } answers {}

        val button = VenmoButton(
            ApplicationProvider.getApplicationContext(),
            mockVenmoClient,
            mockVenmoLauncher
        )
        button.setVenmoRequest(venmoRequest)
        button.performClick()

        // Button should be disabled after click
        assertFalse(button.isEnabled)

        // Simulate async failure from createPaymentAuthRequest
        val failure = mockk<VenmoPaymentAuthRequest.Failure>(relaxed = true)
        every { failure.error } returns Exception("createPaymentAuthRequest failed")
        callbackSlot.captured.onVenmoPaymentAuthRequest(failure)

        // Button should be re-enabled after failure
        assertTrue(button.isEnabled)
    }

    @Test
    fun `setVenmoRequest when getActivity returns null re-enables button`() {
        val callbackSlot = slot<VenmoPaymentAuthRequestCallback>()
        every {
            mockVenmoClient.createPaymentAuthRequest(any(), any(), capture(callbackSlot))
        } answers {}

        // Using Application context (no Activity) so getActivity() returns null
        val button = VenmoButton(
            ApplicationProvider.getApplicationContext(),
            mockVenmoClient,
            mockVenmoLauncher
        )
        button.setVenmoRequest(venmoRequest)
        button.performClick()

        assertFalse(button.isEnabled)

        // Simulate ReadyToLaunch callback, but getActivity() is null
        val readyToLaunch = mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        callbackSlot.captured.onVenmoPaymentAuthRequest(readyToLaunch)

        // Button should be re-enabled after Activity null failure
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp re-enables button on PaymentAuthResult Failure`() {
        val button = VenmoButton(
            ApplicationProvider.getApplicationContext(),
            mockVenmoClient,
            mockVenmoLauncher
        )

        val pendingRequest = VenmoPendingRequest.Started("test-pending-request")
        val paymentAuthFailure = mockk<VenmoPaymentAuthResult.Failure>(relaxed = true)
        every { paymentAuthFailure.error } returns Exception("return to app error")
        every {
            mockVenmoLauncher.handleReturnToApp(any(), any())
        } returns paymentAuthFailure

        // Simulate button was clicked (disabled) during launch flow
        button.isEnabled = false
        assertFalse(button.isEnabled)

        button.handleReturnToApp(
            pendingRequest = pendingRequest,
            intent = Intent(),
            callback = mockk(relaxed = true)
        )

        // Button should be re-enabled after handleReturnToApp
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp re-enables button on PaymentAuthResult NoResult`() {
        val button = VenmoButton(
            ApplicationProvider.getApplicationContext(),
            mockVenmoClient,
            mockVenmoLauncher
        )

        val pendingRequest = VenmoPendingRequest.Started("test-pending-request")
        every {
            mockVenmoLauncher.handleReturnToApp(any(), any())
        } returns VenmoPaymentAuthResult.NoResult

        // Simulate button was clicked (disabled) during launch flow
        button.isEnabled = false
        assertFalse(button.isEnabled)

        button.handleReturnToApp(
            pendingRequest = pendingRequest,
            intent = Intent(),
            callback = mockk(relaxed = true)
        )

        // Button should be re-enabled after handleReturnToApp
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp re-enables button on PaymentAuthResult Success`() {
        val button = VenmoButton(
            ApplicationProvider.getApplicationContext(),
            mockVenmoClient,
            mockVenmoLauncher
        )

        val pendingRequest = VenmoPendingRequest.Started("test-pending-request")
        val paymentAuthSuccess = mockk<VenmoPaymentAuthResult.Success>(relaxed = true)
        every {
            mockVenmoLauncher.handleReturnToApp(any(), any())
        } returns paymentAuthSuccess

        // Simulate button was clicked (disabled) during launch flow
        button.isEnabled = false
        assertFalse(button.isEnabled)

        button.handleReturnToApp(
            pendingRequest = pendingRequest,
            intent = Intent(),
            callback = mockk(relaxed = true)
        )

        // Button should be re-enabled after handleReturnToApp (for all result types)
        assertTrue(button.isEnabled)
    }
}