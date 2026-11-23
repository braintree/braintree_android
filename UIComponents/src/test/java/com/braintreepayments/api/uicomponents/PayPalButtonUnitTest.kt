package com.braintreepayments.api.uicomponents

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalButtonUnitTest {

    private val mockPayPalClient: PayPalClient = mockk(relaxed = true)
    private val mockPayPalLauncher: PayPalLauncher = mockk(relaxed = true)
    private val mockAnalyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val payPalRequest = PayPalCheckoutRequest("1.00", true)
    private val launchCallback: PayPalLaunchCallback = mockk(relaxed = true)
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

    private fun captureAuthRequestCallback(): CapturingSlot<PayPalPaymentAuthCallback> {
        val callbackSlot = slot<PayPalPaymentAuthCallback>()
        every {
            mockPayPalClient.createPaymentAuthRequest(any(), any(), capture(callbackSlot))
        } answers {}
        return callbackSlot
    }

    private fun buttonWithActivity(): PayPalButton {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        return PayPalButton(activity, mockPayPalClient, mockPayPalLauncher).also {
            it.payPalLaunchCallback = launchCallback
            it.setPayPalRequest(payPalRequest)
        }
    }

    @Test
    fun `click disables button and creates a payment auth request with the request`() {
        captureAuthRequestCallback()
        val clickButton = buttonWithActivity()

        clickButton.performClick()

        assertFalse(clickButton.isEnabled)
        verify { mockPayPalClient.createPaymentAuthRequest(any(), payPalRequest, any()) }
    }

    @Test
    fun `click without payPalLaunchCallback throws NullPointerException`() {
        val clickButton = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )
        clickButton.setPayPalRequest(payPalRequest)

        val exception = assertThrows(NullPointerException::class.java) { clickButton.performClick() }
        assertEquals("PayPalLaunchCallback must be initialized first", exception.message)
    }

    @Test
    fun `click when createPaymentAuthRequest returns Failure notifies callback with Failure`() {
        val callbackSlot = captureAuthRequestCallback()
        val clickButton = buttonWithActivity()
        clickButton.performClick()

        val error = Exception("createPaymentAuthRequest failed")
        val failure = mockk<PayPalPaymentAuthRequest.Failure>(relaxed = true)
        every { failure.error } returns error
        callbackSlot.captured.onPayPalPaymentAuthRequest(failure)

        verify {
            launchCallback.onPayPalPaymentAuthRequest(
                match { it is PayPalPendingRequest.Failure && it.error === error }
            )
        }
    }

    @Test
    fun `click when launcher returns Started notifies callback and sends selected event`() {
        val callbackSlot = captureAuthRequestCallback()
        every {
            mockPayPalLauncher.launch(any(), any())
        } returns PayPalPendingRequest.Started("pending-request")
        val clickButton = buttonWithActivity()
        clickButton.performClick()

        val readyToLaunch = mockk<PayPalPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        callbackSlot.captured.onPayPalPaymentAuthRequest(readyToLaunch)

        verify {
            launchCallback.onPayPalPaymentAuthRequest(
                match { it is PayPalPendingRequest.Started && it.pendingRequestString == "pending-request" }
            )
        }
        verify {
            mockAnalyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_XML_VIEW)
            )
        }
    }

    @Test
    fun `click when launcher returns Failure notifies callback with Failure`() {
        val callbackSlot = captureAuthRequestCallback()
        val launchError = Exception("launch failed")
        every {
            mockPayPalLauncher.launch(any(), any())
        } returns PayPalPendingRequest.Failure(launchError)
        val clickButton = buttonWithActivity()
        clickButton.performClick()

        callbackSlot.captured.onPayPalPaymentAuthRequest(
            mockk<PayPalPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )

        verify {
            launchCallback.onPayPalPaymentAuthRequest(
                match { it is PayPalPendingRequest.Failure && it.error === launchError }
            )
        }
    }

    @Test
    fun `click when activity is null notifies callback with Failure`() {
        val callbackSlot = captureAuthRequestCallback()
        // Application context has no Activity, so getActivity() returns null
        val clickButton = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )
        clickButton.payPalLaunchCallback = launchCallback
        clickButton.setPayPalRequest(payPalRequest)
        clickButton.performClick()

        callbackSlot.captured.onPayPalPaymentAuthRequest(
            mockk<PayPalPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )

        verify {
            launchCallback.onPayPalPaymentAuthRequest(
                match {
                    it is PayPalPendingRequest.Failure && it.error is NullPointerException &&
                        it.error.message == "Activity is null"
                }
            )
        }
        verify(exactly = 0) { mockPayPalLauncher.launch(any(), any()) }
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
        val tokenizeResult = mockk<PayPalResult.Success>(relaxed = true)
        every { mockPayPalClient.tokenize(authSuccess, any()) } answers {
            secondArg<PayPalTokenizeCallback>().onPayPalResult(tokenizeResult)
        }

        assertFalse(button.isEnabled)

        button.handleReturnToApp(pendingRequest, intent, callback)

        verify { mockPayPalClient.tokenize(authSuccess, any()) }
        verify { callback.onPayPalResult(tokenizeResult) }
        assertTrue(button.isEnabled)
    }
}
