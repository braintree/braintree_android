package com.braintreepayments.api.uicomponents.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequestCallback
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
import com.braintreepayments.api.uicomponents.VenmoButtonColor
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
class VenmoButtonUnitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val venmoClient: VenmoClient = mockk(relaxed = true)
    private val venmoLauncher: VenmoLauncher = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val repository: PendingRequestRepository = mockk(relaxed = true)
    private val venmoRequest: VenmoRequest = mockk(relaxed = true)
    private val results = mutableListOf<VenmoResult>()
    private val authCallbackSlot = slot<VenmoPaymentAuthRequestCallback>()

    private fun setContent() {
        every {
            venmoClient.createPaymentAuthRequest(any(), any(), capture(authCallbackSlot))
        } returns Unit
        coEvery { repository.getPendingRequest() } returns ""
        composeRule.setContent {
            VenmoButtonContent(
                style = VenmoButtonColor.Blue,
                venmoRequest = venmoRequest,
                pendingRequestRepository = repository,
                venmoTokenizeCallback = VenmoTokenizeCallback { results.add(it) },
                launcherFactory = { venmoLauncher },
                clientFactory = { venmoClient },
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
        every { venmoLauncher.launch(any(), any()) } returns VenmoPendingRequest.Started(pendingRequest)
        click()
        authCallbackSlot.captured.onVenmoPaymentAuthRequest(
            mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
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
                UIComponentsAnalytics.VENMO_BUTTON_PRESENTED,
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
                UIComponentsAnalytics.VENMO_BUTTON_SELECTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
        verify { venmoClient.createPaymentAuthRequest(any(), venmoRequest, any()) }
    }

    @Test
    fun `payment auth request Failure forwards error to callback`() {
        setContent()
        click()
        val error = Exception("auth request failed")
        val failure = mockk<VenmoPaymentAuthRequest.Failure>(relaxed = true)
        every { failure.error } returns error

        authCallbackSlot.captured.onVenmoPaymentAuthRequest(failure)
        composeRule.waitForIdle()

        assertEquals(error, (results.single() as VenmoResult.Failure).error)
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
        every { venmoLauncher.launch(any(), any()) } returns VenmoPendingRequest.Failure(error)
        click()

        authCallbackSlot.captured.onVenmoPaymentAuthRequest(
            mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )
        composeRule.waitForIdle()

        assertEquals(error, (results.single() as VenmoResult.Failure).error)
        coVerify(exactly = 0) { repository.storePendingRequest(any()) }
    }

    @Test
    fun `resume after launch with Success tokenizes and clears pending request`() {
        setContent()
        launchSuccessfully()
        coEvery { repository.getPendingRequest() } returns "pending"
        val authSuccess = mockk<VenmoPaymentAuthResult.Success>(relaxed = true)
        every { venmoLauncher.handleReturnToApp(any(), any()) } returns authSuccess
        val tokenized = mockk<VenmoResult.Success>(relaxed = true)
        every { venmoClient.tokenize(authSuccess, any()) } answers {
            secondArg<VenmoTokenizeCallback>().onVenmoResult(tokenized)
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
        every { venmoLauncher.handleReturnToApp(any(), any()) } returns VenmoPaymentAuthResult.NoResult

        pauseAndResume()

        assertSame(VenmoResult.Cancel, results.single())
    }

    @Test
    fun `resume after launch with Failure forwards error`() {
        setContent()
        launchSuccessfully()
        coEvery { repository.getPendingRequest() } returns "pending"
        val error = Exception("return error")
        val authFailure = mockk<VenmoPaymentAuthResult.Failure>(relaxed = true)
        every { authFailure.error } returns error
        every { venmoLauncher.handleReturnToApp(any(), any()) } returns authFailure

        pauseAndResume()

        assertEquals(error, (results.single() as VenmoResult.Failure).error)
    }

    @Test
    fun `resume after launch with empty pending request fails with PendingRequestException`() {
        setContent()
        launchSuccessfully()
        coEvery { repository.getPendingRequest() } returns ""

        pauseAndResume()

        assertTrue((results.single() as VenmoResult.Failure).error is PendingRequestException)
        verify(exactly = 0) { venmoLauncher.handleReturnToApp(any(), any()) }
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
