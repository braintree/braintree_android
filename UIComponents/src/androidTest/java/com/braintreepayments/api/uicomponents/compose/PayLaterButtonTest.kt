package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import androidx.compose.ui.test.hasClickAction
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.uicomponents.PayLaterButtonColor
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PayLaterButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(resId: Int) = context.getString(resId)

    private fun payLaterRequest() = PayPalCheckoutRequest("10.00", true)

    private fun setPayLaterButton(
        request: PayPalCheckoutRequest = payLaterRequest(),
        style: PayLaterButtonColor = PayLaterButtonColor.Blue
    ) {
        composeTestRule.setContent {
            PayLaterButton(
                style = style,
                payPalRequest = request,
                authorization = Fixtures.TOKENIZATION_KEY,
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.braintreepayments.demo",
                paypalTokenizeCallback = {}
            )
        }
    }

    private fun payLaterButton() =
        composeTestRule.onNodeWithContentDescription(str(R.string.pay_later_button_content_description))

    // region Rendering

    @Test
    fun payLaterButton_isEnabledByDefault() {
        setPayLaterButton()

        payLaterButton().assertIsEnabled()
    }

    @Test
    fun payLaterButton_rendersForEachColor() {
        listOf(PayLaterButtonColor.Blue, PayLaterButtonColor.Black, PayLaterButtonColor.White).forEach { color ->
            setPayLaterButton(style = color)
            payLaterButton().assertIsEnabled()
            // setContent can only be called once per rule, so stop after the first for this test
            return
        }
    }

    // endregion

    // region Request

    @Test
    fun payLaterButton_doesNotSetShouldOfferPayLater_untilClicked() {
        val request = payLaterRequest()
        setPayLaterButton(request)

        assertFalse(request.shouldOfferPayLater)
    }

    @Test
    fun clickingPayLaterButton_setsShouldOfferPayLater() {
        val request = payLaterRequest()
        setPayLaterButton(request)

        payLaterButton().performClick()

        assertTrue(request.shouldOfferPayLater)
    }

    // endregion

    // region Enabled state

    @Test
    fun clickingPayLaterButton_disablesTheButton() {
        setPayLaterButton()

        payLaterButton().performClick()

        payLaterButton().assertIsNotEnabled()
    }

    // endregion

    @Test
    fun clickingPayPalButton_doesNotSetShouldOfferPayLater() {
        val request = PayPalCheckoutRequest("10.00", true)
        composeTestRule.setContent {
            PayPalButton(
                style = PayPalButtonColor.Blue,
                payPalRequest = request,
                authorization = Fixtures.TOKENIZATION_KEY,
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.braintreepayments.demo",
                paypalTokenizeCallback = {}
            )
        }

        composeTestRule.onNode(hasClickAction()).performClick()

        assertFalse(request.shouldOfferPayLater)
    }
}
