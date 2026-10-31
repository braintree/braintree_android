package com.braintreepayments.api.uicomponents.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalButtonUnitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val payPalClient: PayPalClient = mockk(relaxed = true)
    private val payPalLauncher: PayPalLauncher = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val repository: PendingRequestRepository = mockk(relaxed = true)
    private val payPalRequest: PayPalRequest = mockk(relaxed = true)
    private val results = mutableListOf<PayPalResult>()
    private val authCallbackSlot = slot<PayPalPaymentAuthCallback>()

    private fun setContent() {
        every {
            payPalClient.createPaymentAuthRequest(any(), any(), capture(authCallbackSlot))
        } returns Unit
        coEvery { repository.getPendingRequest() } returns ""
        composeRule.setContent {
            PayPalButtonContent(
                style = PayPalButtonColor.Blue,
                payPalRequest = payPalRequest,
                pendingRequestRepository = repository,
                paypalTokenizeCallback = PayPalTokenizeCallback { results.add(it) },
                launcherFactory = { payPalLauncher },
                clientFactory = { payPalClient },
                analyticsClientProvider = { analyticsClient }
            )
        }
    }

    private fun click() {
        composeRule.onRoot().performClick()
        composeRule.waitForIdle()
    }

    private fun pauseAndResume() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    private fun launchSuccessfully(pendingRequest: String = "pending") {
        every { payPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Started(pendingRequest)
        click()
        authCallbackSlot.captured.onPayPalPaymentAuthRequest(
            mockk<PayPalPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )
        composeRule.waitForIdle()
    }

    @Test
    fun `sends presented event once with compose ui type`() {
        setContent()
        composeRule.waitForIdle()
        pauseAndResume()

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `click sends selected event and creates payment auth request`() {
        setContent()
        click()

        verify {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
        verify { payPalClient.createPaymentAuthRequest(any(), payPalRequest, any()) }
    }

    @Test
    fun `payment auth request Failure forwards error to callback`() {
        setContent()
        click()
        val error = Exception("auth request failed")
        val failure = mockk<PayPalPaymentAuthRequest.Failure>(relaxed = true)
        every { failure.error } returns error

        authCallbackSlot.captured.onPayPalPaymentAuthRequest(failure)
        composeRule.waitForIdle()

        assertEquals(error, (results.single() as PayPalResult.Failure).error)
    }

    @Test
    fun `launcher Started stores pending request`() {
        setContent()
        launchSuccessfully("pending-request")

        coVerify { repository.storePendingRequest("pending-request") }
        assertTrue(results.isEmpty())
    }

    @Test
    fun `launcher Failure forwards error to callback`() {
        setContent()
        val error = Exception("launch failed")
        every { payPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Failure(error)
        click()

        authCallbackSlot.captured.onPayPalPaymentAuthRequest(
            mockk<PayPalPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )
        composeRule.waitForIdle()

        assertEquals(error, (results.single() as PayPalResult.Failure).error)
        coVerify(exactly = 0) { repository.storePendingRequest(any()) }
    }

    @Test
    fun `resume after launch with Success tokenizes and clears pending request`() {
        setContent()
        launchSuccessfully()
        coEvery { repository.getPendingRequest() } returns "pending"
        val authSuccess = mockk<PayPalPaymentAuthResult.Success>(relaxed = true)
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns authSuccess
        val tokenized = mockk<PayPalResult.Success>(relaxed = true)
        every { payPalClient.tokenize(authSuccess, any()) } answers {
            secondArg<PayPalTokenizeCallback>().onPayPalResult(tokenized)
        }

        pauseAndResume()

        assertSame(tokenized, results.single())
        coVerify { repository.clearPendingRequest() }
    }

    @Test
    fun `resume after launch with NoResult returns Cancel`() {
        setContent()
        launchSuccessfully()
        coEvery { repository.getPendingRequest() } returns "pending"
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult

        pauseAndResume()

        assertSame(PayPalResult.Cancel, results.single())
    }

    @Test
    fun `resume after launch with Failure forwards error`() {
        setContent()
        launchSuccessfully()
        coEvery { repository.getPendingRequest() } returns "pending"
        val error = Exception("return error")
        val authFailure = mockk<PayPalPaymentAuthResult.Failure>(relaxed = true)
        every { authFailure.error } returns error
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns authFailure

        pauseAndResume()

        assertEquals(error, (results.single() as PayPalResult.Failure).error)
    }

    @Test
    fun `resume after launch with empty pending request fails with PendingRequestException`() {
        setContent()
        launchSuccessfully()
        coEvery { repository.getPendingRequest() } returns ""

        pauseAndResume()

        assertTrue((results.single() as PayPalResult.Failure).error is PendingRequestException)
        verify(exactly = 0) { payPalLauncher.handleReturnToApp(any(), any()) }
    }

    @Test
    fun `resume without a launched flow does nothing`() {
        setContent()
        composeRule.waitForIdle()

        pauseAndResume()

        assertTrue(results.isEmpty())
        coVerify(exactly = 0) { repository.getPendingRequest() }
    }
}
