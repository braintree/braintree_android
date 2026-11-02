package com.braintreepayments.api.uicomponents

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequestCallback
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VenmoButtonUnitTest {

    private val mockVenmoClient: VenmoClient = mockk(relaxed = true)
    private val mockVenmoLauncher: VenmoLauncher = mockk(relaxed = true)
    private val mockAnalyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val launchCallback: VenmoLaunchCallback = mockk(relaxed = true)
    private val venmoRequest = VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE)

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(mockAnalyticsClient)
    }

    @After
    fun tearDown() {
        unmockkObject(AnalyticsClient.Companion)
    }

    private fun captureAuthRequestCallback(): CapturingSlot<VenmoPaymentAuthRequestCallback> {
        val callbackSlot = slot<VenmoPaymentAuthRequestCallback>()
        every {
            mockVenmoClient.createPaymentAuthRequest(any(), any(), capture(callbackSlot))
        } answers {}
        return callbackSlot
    }

    private fun buttonWithActivity(): VenmoButton {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        return VenmoButton(activity, mockVenmoClient, mockVenmoLauncher).also {
            it.venmoLaunchCallback = launchCallback
            it.setVenmoRequest(venmoRequest)
        }
    }

    private fun buttonWithoutActivity(): VenmoButton =
        VenmoButton(ApplicationProvider.getApplicationContext(), mockVenmoClient, mockVenmoLauncher).also {
            it.venmoLaunchCallback = launchCallback
            it.setVenmoRequest(venmoRequest)
        }

    @Test
    fun `click disables the button and creates a payment auth request with the request`() {
        captureAuthRequestCallback()
        val button = buttonWithActivity()

        button.performClick()

        assertFalse(button.isEnabled)
        verify { mockVenmoClient.createPaymentAuthRequest(any(), venmoRequest, any()) }
    }

    @Test
    fun `payment auth request Failure notifies callback with the error`() {
        val callbackSlot = captureAuthRequestCallback()
        val button = buttonWithActivity()
        button.performClick()
        val error = Exception("createPaymentAuthRequest failed")
        val failure = mockk<VenmoPaymentAuthRequest.Failure>(relaxed = true)
        every { failure.error } returns error

        callbackSlot.captured.onVenmoPaymentAuthRequest(failure)

        verify {
            launchCallback.onVenmoPaymentAuthRequest(
                match { it is VenmoPendingRequest.Failure && it.error === error }
            )
        }
    }

    @Test
    fun `launcher Started notifies callback and sends selected event`() {
        val callbackSlot = captureAuthRequestCallback()
        every { mockVenmoLauncher.launch(any(), any()) } returns VenmoPendingRequest.Started("pending-request")
        val button = buttonWithActivity()
        button.performClick()

        callbackSlot.captured.onVenmoPaymentAuthRequest(
            mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )

        verify {
            launchCallback.onVenmoPaymentAuthRequest(
                match { it is VenmoPendingRequest.Started && it.pendingRequestString == "pending-request" }
            )
        }
        verify {
            mockAnalyticsClient.sendEvent(
                UIComponentsAnalytics.VENMO_BUTTON_SELECTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_XML_VIEW)
            )
        }
        assertFalse(button.isEnabled)
    }

    @Test
    fun `launcher Failure notifies callback with the error`() {
        val callbackSlot = captureAuthRequestCallback()
        val launchError = Exception("launch failed")
        every { mockVenmoLauncher.launch(any(), any()) } returns VenmoPendingRequest.Failure(launchError)
        val button = buttonWithActivity()
        button.performClick()

        callbackSlot.captured.onVenmoPaymentAuthRequest(
            mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )

        verify {
            launchCallback.onVenmoPaymentAuthRequest(
                match { it is VenmoPendingRequest.Failure && it.error === launchError }
            )
        }
    }

    @Test
    fun `missing activity notifies callback with Activity is null and does not launch`() {
        val callbackSlot = captureAuthRequestCallback()
        val button = buttonWithoutActivity()
        button.performClick()

        callbackSlot.captured.onVenmoPaymentAuthRequest(
            mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )

        verify {
            launchCallback.onVenmoPaymentAuthRequest(
                match {
                    it is VenmoPendingRequest.Failure && it.error is NullPointerException &&
                        it.error.message == "Activity is null"
                }
            )
        }
        verify(exactly = 0) { mockVenmoLauncher.launch(any(), any()) }
    }

    @Test
    fun `handleReturnToApp on Success tokenizes and forwards result`() {
        val paymentAuthSuccess = mockk<VenmoPaymentAuthResult.Success>(relaxed = true)
        every { mockVenmoLauncher.handleReturnToApp(any(), any()) } returns paymentAuthSuccess
        val tokenizeResult = mockk<VenmoResult.Success>(relaxed = true)
        every { mockVenmoClient.tokenize(paymentAuthSuccess, any()) } answers {
            secondArg<VenmoTokenizeCallback>().onVenmoResult(tokenizeResult)
        }
        val callback = mockk<VenmoTokenizeCallback>(relaxed = true)
        val button = buttonWithoutActivity()
        button.isEnabled = false

        button.handleReturnToApp(VenmoPendingRequest.Started("pending"), Intent(), callback)

        verify { callback.onVenmoResult(tokenizeResult) }
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on NoResult returns Cancel`() {
        every { mockVenmoLauncher.handleReturnToApp(any(), any()) } returns VenmoPaymentAuthResult.NoResult
        val resultSlot = slot<VenmoResult>()
        val button = buttonWithoutActivity()
        button.isEnabled = false

        button.handleReturnToApp(VenmoPendingRequest.Started("pending"), Intent()) { resultSlot.captured = it }

        assertSame(VenmoResult.Cancel, resultSlot.captured)
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on Failure returns Failure with the error`() {
        val error = Exception("return to app error")
        val paymentAuthFailure = mockk<VenmoPaymentAuthResult.Failure>(relaxed = true)
        every { paymentAuthFailure.error } returns error
        every { mockVenmoLauncher.handleReturnToApp(any(), any()) } returns paymentAuthFailure
        val resultSlot = slot<VenmoResult>()
        val button = buttonWithoutActivity()
        button.isEnabled = false

        button.handleReturnToApp(VenmoPendingRequest.Started("pending"), Intent()) { resultSlot.captured = it }

        assertEquals(error, (resultSlot.captured as VenmoResult.Failure).error)
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp passes the pending request and intent to the launcher`() {
        every { mockVenmoLauncher.handleReturnToApp(any(), any()) } returns VenmoPaymentAuthResult.NoResult
        val button = buttonWithoutActivity()
        val pendingRequest = VenmoPendingRequest.Started("pending")
        val intent = Intent("test")

        button.handleReturnToApp(pendingRequest, intent) {}

        verify { mockVenmoLauncher.handleReturnToApp(pendingRequest, intent) }
    }
}
