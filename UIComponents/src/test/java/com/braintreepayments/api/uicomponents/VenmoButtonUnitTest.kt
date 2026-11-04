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
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
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

    @Test
    fun `click disables the button and creates a payment auth request with the request`() {
        every { mockVenmoClient.createPaymentAuthRequest(any(), any(), any()) } answers {}
        val button = VenmoButton(ApplicationProvider.getApplicationContext(), mockVenmoClient, mockVenmoLauncher)
        button.setVenmoRequest(venmoRequest)

        button.performClick()

        assertFalse(button.isEnabled)
        verify { mockVenmoClient.createPaymentAuthRequest(any(), venmoRequest, any()) }
    }

    @Test
    fun `launcher Started notifies callback and sends selected event`() {
        val callbackSlot = slot<VenmoPaymentAuthRequestCallback>()
        every {
            mockVenmoClient.createPaymentAuthRequest(any(), any(), capture(callbackSlot))
        } answers {}
        every { mockVenmoLauncher.launch(any(), any()) } returns VenmoPendingRequest.Started("pending-request")
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val button = VenmoButton(activity, mockVenmoClient, mockVenmoLauncher)
        button.venmoLaunchCallback = launchCallback
        button.setVenmoRequest(venmoRequest)
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
    fun `handleReturnToApp passes the pending request and intent to the launcher`() {
        every { mockVenmoLauncher.handleReturnToApp(any(), any()) } returns VenmoPaymentAuthResult.NoResult
        val button = VenmoButton(ApplicationProvider.getApplicationContext(), mockVenmoClient, mockVenmoLauncher)
        val pendingRequest = VenmoPendingRequest.Started("pending")
        val intent = Intent("test")

        button.handleReturnToApp(pendingRequest, intent) {}

        verify { mockVenmoLauncher.handleReturnToApp(pendingRequest, intent) }
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
        button.venmoLaunchCallback = launchCallback
        button.setVenmoRequest(venmoRequest)
        button.performClick()

        // Button should be disabled after click
        assertFalse(button.isEnabled)

        // Simulate async failure from createPaymentAuthRequest
        val failure = mockk<VenmoPaymentAuthRequest.Failure>(relaxed = true)
        val error = Exception("createPaymentAuthRequest failed")
        every { failure.error } returns error
        callbackSlot.captured.onVenmoPaymentAuthRequest(failure)

        verify {
            launchCallback.onVenmoPaymentAuthRequest(
                match { it is VenmoPendingRequest.Failure && it.error === error }
            )
        }
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
        button.venmoLaunchCallback = launchCallback
        button.setVenmoRequest(venmoRequest)
        button.performClick()

        assertFalse(button.isEnabled)

        // Simulate ReadyToLaunch callback, but getActivity() is null
        val readyToLaunch = mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        callbackSlot.captured.onVenmoPaymentAuthRequest(readyToLaunch)

        verify {
            launchCallback.onVenmoPaymentAuthRequest(
                match {
                    it is VenmoPendingRequest.Failure && it.error is NullPointerException &&
                        it.error.message == "Activity is null"
                }
            )
        }
        verify(exactly = 0) { mockVenmoLauncher.launch(any(), any()) }
        // Button should be re-enabled after Activity null failure
        assertTrue(button.isEnabled)
    }

    @Test
    fun `setVenmoRequest when launcher returns Failure re-enables button`() {
        val callbackSlot = slot<VenmoPaymentAuthRequestCallback>()
        every {
            mockVenmoClient.createPaymentAuthRequest(any(), any(), capture(callbackSlot))
        } answers {}

        val launchError = Exception("launch failed")
        every {
            mockVenmoLauncher.launch(any(), any())
        } returns VenmoPendingRequest.Failure(launchError)

        // Using a ComponentActivity context so getActivity() returns non-null
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val button = VenmoButton(activity, mockVenmoClient, mockVenmoLauncher)
        button.venmoLaunchCallback = launchCallback
        button.setVenmoRequest(venmoRequest)
        button.performClick()

        assertFalse(button.isEnabled)

        // Simulate ReadyToLaunch callback; launcher.launch() then returns Failure
        val readyToLaunch = mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        callbackSlot.captured.onVenmoPaymentAuthRequest(readyToLaunch)

        verify { mockVenmoLauncher.launch(activity, readyToLaunch) }
        verify {
            launchCallback.onVenmoPaymentAuthRequest(
                match { it is VenmoPendingRequest.Failure && it.error === launchError }
            )
        }
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
        val returnError = Exception("return to app error")
        every { paymentAuthFailure.error } returns returnError
        every {
            mockVenmoLauncher.handleReturnToApp(any(), any())
        } returns paymentAuthFailure

        // Simulate button was clicked (disabled) during launch flow
        button.isEnabled = false
        assertFalse(button.isEnabled)

        val failureResult = slot<VenmoResult>()
        button.handleReturnToApp(pendingRequest, Intent()) { failureResult.captured = it }

        assertEquals(returnError, (failureResult.captured as VenmoResult.Failure).error)

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

        val noResult = slot<VenmoResult>()
        button.handleReturnToApp(pendingRequest, Intent()) { noResult.captured = it }

        assertSame(VenmoResult.Cancel, noResult.captured)

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

        val tokenizeResult = mockk<VenmoResult.Success>(relaxed = true)
        every { mockVenmoClient.tokenize(paymentAuthSuccess, any()) } answers {
            secondArg<VenmoTokenizeCallback>().onVenmoResult(tokenizeResult)
        }
        val callback = mockk<VenmoTokenizeCallback>(relaxed = true)
        button.handleReturnToApp(pendingRequest, Intent(), callback)

        verify { callback.onVenmoResult(tokenizeResult) }

        // Button should be re-enabled after handleReturnToApp (for all result types)
        assertTrue(button.isEnabled)
    }
}
