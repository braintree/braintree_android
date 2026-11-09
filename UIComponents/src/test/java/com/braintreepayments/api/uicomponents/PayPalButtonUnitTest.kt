package com.braintreepayments.api.uicomponents

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalButtonUnitTest {

    private val mockPayPalClient: PayPalClient = mockk(relaxed = true)
    private val mockPayPalLauncher: PayPalLauncher = mockk(relaxed = true)
    private val mockAnalyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val callback: PayPalTokenizeCallback = mockk(relaxed = true)

    private val pendingRequest = PayPalPendingRequest.Started("test-pending-request")
    private val intent = Intent()

    private lateinit var button: PayPalButton

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(mockAnalyticsClient)

        button = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )

        // Simulate the button having been clicked (disabled) during the launch flow
        button.isEnabled = false
    }

    @After
    fun tearDown() {
        unmockkObject(AnalyticsClient.Companion)
    }

    @Test
    fun `handleReturnToApp restores the pending request before completing the flow`() {
        every { mockPayPalLauncher.restorePendingRequest(any()) } just runs
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult

        button.handleReturnToApp(pendingRequest, intent, callback)

        verifyOrder {
            mockPayPalLauncher.restorePendingRequest("test-pending-request")
            mockPayPalLauncher.handleReturnToApp(pendingRequest, intent)
        }
    }

    @Test
    fun `handleReturnToApp when restore fails reports failure, re-enables button and does not complete the flow`() {
        val restoreError = BrowserSwitchException("invalid pending request")
        every { mockPayPalLauncher.restorePendingRequest(any()) } throws restoreError

        button.handleReturnToApp(pendingRequest, intent, callback)

        verify { callback.onPayPalResult(match { it is PayPalResult.Failure && it.error === restoreError }) }
        verify(exactly = 0) { mockPayPalLauncher.handleReturnToApp(any(), any()) }
        verify(exactly = 0) { mockPayPalClient.tokenize(any(), any()) }
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on NoResult reports cancel and re-enables button`() {
        every { mockPayPalLauncher.restorePendingRequest(any()) } just runs
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult

        button.handleReturnToApp(pendingRequest, intent, callback)

        verify { callback.onPayPalResult(PayPalResult.Cancel) }
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on Failure reports failure and re-enables button`() {
        val authError = Exception("return to app error")
        val authFailure = mockk<PayPalPaymentAuthResult.Failure>(relaxed = true)
        every { authFailure.error } returns authError
        every { mockPayPalLauncher.restorePendingRequest(any()) } just runs
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns authFailure

        button.handleReturnToApp(pendingRequest, intent, callback)

        verify { callback.onPayPalResult(match { it is PayPalResult.Failure && it.error === authError }) }
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on Success tokenizes and re-enables button`() {
        val authSuccess = mockk<PayPalPaymentAuthResult.Success>(relaxed = true)
        every { mockPayPalLauncher.restorePendingRequest(any()) } just runs
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns authSuccess

        assertFalse(button.isEnabled)

        button.handleReturnToApp(pendingRequest, intent, callback)

        verify { mockPayPalClient.tokenize(authSuccess, any()) }
        assertTrue(button.isEnabled)
    }
}
