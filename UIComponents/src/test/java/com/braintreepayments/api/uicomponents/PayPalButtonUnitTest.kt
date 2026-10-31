package com.braintreepayments.api.uicomponents

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(mockAnalyticsClient)
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
    fun `click disables button`() {
        captureAuthRequestCallback()
        val button = buttonWithActivity()

        button.performClick()

        assertFalse(button.isEnabled)
    }

    @Test
    fun `click without payPalLaunchCallback throws NullPointerException`() {
        val button = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )
        button.setPayPalRequest(payPalRequest)

        assertThrows(NullPointerException::class.java) { button.performClick() }
    }

    @Test
    fun `click when createPaymentAuthRequest returns Failure notifies callback with Failure`() {
        val callbackSlot = captureAuthRequestCallback()
        val button = buttonWithActivity()
        button.performClick()

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
        val button = buttonWithActivity()
        button.performClick()

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
        val button = buttonWithActivity()
        button.performClick()

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
        val button = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )
        button.payPalLaunchCallback = launchCallback
        button.setPayPalRequest(payPalRequest)
        button.performClick()

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
    fun `handleReturnToApp on PaymentAuthResult Success tokenizes and forwards result`() {
        val paymentAuthSuccess = mockk<PayPalPaymentAuthResult.Success>(relaxed = true)
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns paymentAuthSuccess
        val tokenizeResult = mockk<PayPalResult.Success>(relaxed = true)
        every { mockPayPalClient.tokenize(paymentAuthSuccess, any()) } answers {
            secondArg<com.braintreepayments.api.paypal.PayPalTokenizeCallback>().onPayPalResult(tokenizeResult)
        }
        val callback = mockk<PayPalTokenizeCallback>(relaxed = true)
        val button = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent(), callback)

        verify { callback.onPayPalResult(tokenizeResult) }
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on PaymentAuthResult NoResult returns Cancel`() {
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult
        val resultSlot = slot<PayPalResult>()
        val callback = PayPalTokenizeCallback { resultSlot.captured = it }
        val button = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent(), callback)

        assertSame(PayPalResult.Cancel, resultSlot.captured)
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on PaymentAuthResult Failure returns Failure`() {
        val error = Exception("return to app error")
        val paymentAuthFailure = mockk<PayPalPaymentAuthResult.Failure>(relaxed = true)
        every { paymentAuthFailure.error } returns error
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns paymentAuthFailure
        val resultSlot = slot<PayPalResult>()
        val callback = PayPalTokenizeCallback { resultSlot.captured = it }
        val button = PayPalButton(
            ApplicationProvider.getApplicationContext(),
            mockPayPalClient,
            mockPayPalLauncher
        )
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent(), callback)

        val result = resultSlot.captured as PayPalResult.Failure
        assertEquals(error, result.error)
        assertTrue(button.isEnabled)
    }
}
