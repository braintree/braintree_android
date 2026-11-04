package com.braintreepayments.api.uicomponents.compose

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.uicomponents.PayLaterButtonColor
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayLaterButtonUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val analyticsClient = mockk<AnalyticsClient>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(analyticsClient)
    }

    @After
    fun tearDown() {
        unmockkObject(AnalyticsClient.Companion)
    }

    // region Analytics

    @Test
    fun `sends presented event once when shown`() {
        setPayLaterButton()
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

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

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.pay_later_button_content_description))
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
        composeTestRule.setContent {
            PayLaterButton(
                style = PayLaterButtonColor.Blue,
                payPalRequest = PayPalCheckoutRequest("10.00", true),
                authorization = "sandbox_tokenization_key",
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.braintreepayments.demo",
                pendingRequestRepository = mockk(relaxed = true),
                paypalTokenizeCallback = {}
            )
        }
    }
}
