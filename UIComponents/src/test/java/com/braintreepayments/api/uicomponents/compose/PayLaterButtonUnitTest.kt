package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayLaterButtonColor
import com.braintreepayments.api.uicomponents.R
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers what [PayLaterButton] adds on top of [PayPalButtonFlow] (which has its own tests): Pay Later opt-in,
 * the Pay Later button and delegation. The flow constructs its own client and launcher, so those are mocked.
 */
@RunWith(RobolectricTestRunner::class)
class PayLaterButtonUnitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val repository: PendingRequestRepository = mockk(relaxed = true)
    private val results = mutableListOf<PayPalResult>()
    private val authCallbackSlot = slot<PayPalPaymentAuthCallback>()
    private val requestSlot = slot<PayPalRequest>()
    private var shouldOfferPayLaterAtCreate: Boolean? = null
    private val analyticsClient = mockk<AnalyticsClient>(relaxed = true)

    @Before
    fun setUp() {
        mockkConstructor(PayPalClient::class, PayPalLauncher::class)
        every {
            anyConstructed<PayPalClient>().createPaymentAuthRequest(
                any(), capture(requestSlot), capture(authCallbackSlot)
            )
        } answers {
            shouldOfferPayLaterAtCreate = (requestSlot.captured as PayPalCheckoutRequest).shouldOfferPayLater
        }
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(analyticsClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setContent(request: PayPalCheckoutRequest) {
        composeRule.setContent {
            PayLaterButton(
                style = PayLaterButtonColor.Blue,
                payPalRequest = request,
                authorization = "sandbox_tokenization_key",
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.example.app",
                pendingRequestRepository = repository,
                paypalTokenizeCallback = PayPalTokenizeCallback { results.add(it) }
            )
        }
        composeRule.waitForIdle()
    }

    private fun button() = composeRule.onNodeWithContentDescription(
        composeRule.activity.getString(R.string.pay_later_button_content_description)
    )

    private fun click() {
        button().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `does not opt in to Pay Later before the button is clicked`() {
        val request = PayPalCheckoutRequest("10.00", false)

        setContent(request)

        assertFalse(request.shouldOfferPayLater)
    }

    @Test
    fun `click opts in to Pay Later before creating the payment auth request`() {
        val request = PayPalCheckoutRequest("10.00", false)
        setContent(request)

        click()

        assertEquals(true, shouldOfferPayLaterAtCreate)
        verify(exactly = 1) { anyConstructed<PayPalClient>().createPaymentAuthRequest(any(), request, any()) }
    }

    @Test
    fun `click disables the button while the flow is in progress`() {
        setContent(PayPalCheckoutRequest("10.00", true))
        button().assertIsEnabled()

        click()

        button().assertIsNotEnabled()
    }

    @Test
    fun `payment auth request Failure is forwarded to the callback and re-enables the button`() {
        setContent(PayPalCheckoutRequest("10.00", true))
        click()
        val error = Exception("auth request failed")
        val failure = mockk<PayPalPaymentAuthRequest.Failure>(relaxed = true)
        every { failure.error } returns error

        authCallbackSlot.captured.onPayPalPaymentAuthRequest(failure)
        composeRule.waitForIdle()

        assertEquals(error, (results.single() as PayPalResult.Failure).error)
        button().assertIsEnabled()
    }

    @Test
    fun `no result is reported until the flow completes`() {
        setContent(PayPalCheckoutRequest("10.00", true))

        click()

        assertTrue(results.isEmpty())
    }

    // region Analytics

    @Test
    fun `sends presented event once when shown`() {
        setPayLaterButton()
        composeRule.waitForIdle()
        composeRule.waitForIdle()

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAY_LATER_BUTTON_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
        verify(exactly = 0) { analyticsClient.sendEvent(UIComponentsAnalytics.PAY_LATER_BUTTON_SELECTED, any(), any()) }
    }

    @Test
    fun `sends selected event when clicked`() {
        setPayLaterButton()

        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.pay_later_button_content_description))
            .performClick()

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAY_LATER_BUTTON_SELECTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    // endregion

    private fun setPayLaterButton() {
        setContent(PayPalCheckoutRequest("10.00", true))
    }
}
