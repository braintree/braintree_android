package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
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
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers what [PayPalButton] adds on top of [PayPalButtonFlow] (which has its own tests): analytics and delegation.
 * The flow constructs its own client and launcher, so those constructors are mocked.
 */
@RunWith(RobolectricTestRunner::class)
class PayPalButtonUnitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val repository: PendingRequestRepository = mockk(relaxed = true)
    private val payPalRequest: PayPalRequest = mockk(relaxed = true)
    private val results = mutableListOf<PayPalResult>()
    private val authCallbackSlot = slot<PayPalPaymentAuthCallback>()

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(analyticsClient)
        mockkConstructor(PayPalClient::class, PayPalLauncher::class)
        every {
            anyConstructed<PayPalClient>().createPaymentAuthRequest(any(), any(), capture(authCallbackSlot))
        } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setContent() {
        composeRule.setContent {
            PayPalButton(
                style = PayPalButtonColor.Blue,
                payPalRequest = payPalRequest,
                authorization = "sandbox_tokenization_key",
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.example.app",
                pendingRequestRepository = repository,
                paypalTokenizeCallback = PayPalTokenizeCallback { results.add(it) }
            )
        }
        composeRule.waitForIdle()
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

    @Test
    fun `sends presented event once with compose ui type`() {
        setContent()
        pauseAndResume()

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `does not send selected event before the button is clicked`() {
        setContent()

        verify(exactly = 0) {
            analyticsClient.sendEvent(UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED, any())
        }
    }

    @Test
    fun `click sends selected event and creates payment auth request with the request`() {
        setContent()

        click()

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
        verify { anyConstructed<PayPalClient>().createPaymentAuthRequest(any(), payPalRequest, any()) }
    }

    @Test
    fun `payment auth request Failure is forwarded to the tokenize callback`() {
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
    fun `no result is reported until the flow completes`() {
        setContent()
        click()

        assertTrue(results.isEmpty())
    }
}