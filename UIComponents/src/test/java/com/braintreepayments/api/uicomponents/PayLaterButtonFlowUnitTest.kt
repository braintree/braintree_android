package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Flow-level tests for [PayLaterButton]: launch, return-to-app and styling behavior not covered by
 * [PayLaterButtonUnitTest].
 */
@RunWith(RobolectricTestRunner::class)
class PayLaterButtonFlowUnitTest {

    private lateinit var context: Context

    private val payPalClient: PayPalClient = mockk(relaxed = true)
    private val payPalLauncher: PayPalLauncher = mockk(relaxed = true)
    private val authCallback = slot<PayPalPaymentAuthCallback>()
    private val launchResults = mutableListOf<PayPalPendingRequest>()
    private val request = PayPalCheckoutRequest("10.00", true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        every { payPalClient.createPaymentAuthRequest(any(), any(), capture(authCallback)) } answers {}
    }

    private fun buttonWith(buttonContext: Context, withCallback: Boolean = true) =
        PayLaterButton(buttonContext).apply {
            payPalClient = this@PayLaterButtonFlowUnitTest.payPalClient
            payPalLauncher = this@PayLaterButtonFlowUnitTest.payPalLauncher
            if (withCallback) payPalLaunchCallback = PayPalLaunchCallback { launchResults.add(it) }
            setPayPalRequest(request)
        }

    private fun activity() = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    private fun fillColor(button: PayLaterButton): Int {
        val layers = button.background as LayerDrawable
        return (layers.getDrawable(1) as GradientDrawable).color!!.defaultColor
    }

    @Test
    fun `click without a launch callback throws NullPointerException`() {
        val button = buttonWith(activity(), withCallback = false)

        assertThrows(NullPointerException::class.java) { button.performClick() }
    }

    @Test
    fun `click creates the payment auth request with the checkout request`() {
        val button = buttonWith(activity())

        button.performClick()

        verify { payPalClient.createPaymentAuthRequest(any(), request, any()) }
    }

    @Test
    fun `auth request failure forwards the error to the callback`() {
        val button = buttonWith(activity())
        button.performClick()
        val error = Exception("auth request failed")
        val failure = mockk<PayPalPaymentAuthRequest.Failure>()
        every { failure.error } returns error

        authCallback.captured.onPayPalPaymentAuthRequest(failure)

        assertSame(error, (launchResults.single() as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `launch started forwards the pending request and keeps the button loading`() {
        every { payPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Started("pending-request")
        val button = buttonWith(activity())
        button.performClick()

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        assertEquals("pending-request", (launchResults.single() as PayPalPendingRequest.Started).pendingRequestString)
        assertFalse(button.isEnabled)
    }

    @Test
    fun `launch failure forwards the launcher error`() {
        val error = Exception("launch failed")
        every { payPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Failure(error)
        val button = buttonWith(activity())
        button.performClick()

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        assertSame(error, (launchResults.single() as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `missing activity forwards an Activity is null error and never launches`() {
        val button = buttonWith(context)
        button.performClick()

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        val error = (launchResults.single() as PayPalPendingRequest.Failure).error
        assertTrue(error is NullPointerException)
        assertEquals("Activity is null", error.message)
        verify(exactly = 0) { payPalLauncher.launch(any(), any()) }
    }

    @Test
    fun `handleReturnToApp on Success tokenizes, forwards the result and re-enables`() {
        val authSuccess = mockk<PayPalPaymentAuthResult.Success>(relaxed = true)
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns authSuccess
        val tokenized = mockk<PayPalResult.Success>(relaxed = true)
        every { payPalClient.tokenize(authSuccess, any()) } answers {
            secondArg<PayPalTokenizeCallback>().onPayPalResult(tokenized)
        }
        val results = mutableListOf<PayPalResult>()
        val button = buttonWith(context)
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent()) { results.add(it) }

        assertSame(tokenized, results.single())
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on NoResult returns Cancel and re-enables`() {
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult
        val results = mutableListOf<PayPalResult>()
        val button = buttonWith(context)
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent()) { results.add(it) }

        assertSame(PayPalResult.Cancel, results.single())
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on Failure returns the error and re-enables`() {
        val error = Exception("return error")
        val authFailure = mockk<PayPalPaymentAuthResult.Failure>()
        every { authFailure.error } returns error
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns authFailure
        val results = mutableListOf<PayPalResult>()
        val button = buttonWith(context)
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent()) { results.add(it) }

        assertSame(error, (results.single() as PayPalResult.Failure).error)
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp passes the pending request and intent to the launcher`() {
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult
        val button = buttonWith(context)
        val pending = PayPalPendingRequest.Started("pending")
        val intent = Intent("test")

        button.handleReturnToApp(pending, intent) {}

        verify { payPalLauncher.handleReturnToApp(pending, intent) }
    }

    @Test
    fun `setButtonColor recolors the background`() {
        val button = PayLaterButton(context)
        val blueFill = fillColor(button)

        button.setButtonColor(PayLaterButtonColor.Black)

        assertNotEquals(blueFill, fillColor(button))
        assertEquals(
            ContextCompat.getColor(context, PayLaterButtonColor.Black.toStyle().default.fill),
            fillColor(button)
        )
    }

    @Test
    fun `setButtonColor with the current color leaves the background unchanged`() {
        val button = PayLaterButton(context)
        val blueFill = fillColor(button)

        button.setButtonColor(PayLaterButtonColor.Blue)

        assertEquals(blueFill, fillColor(button))
    }

    @Test
    fun `color keys match the attribute values`() {
        assertEquals(0, PayLaterButtonColor.Blue.key)
        assertEquals(1, PayLaterButtonColor.Black.key)
        assertEquals(2, PayLaterButtonColor.White.key)
    }
}
