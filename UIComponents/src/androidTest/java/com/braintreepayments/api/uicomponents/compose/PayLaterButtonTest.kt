package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.uicomponents.PayLaterButtonColor
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
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
    fun payLaterButton_hasButtonRole() {
        setPayLaterButton()

        payLaterButton().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    // endregion

    // region PayLaterButtonView

    @Test
    fun payLaterButtonView_isDisplayedAndEnabled_forEachColor() {
        val color = mutableStateOf<PayLaterButtonColor>(PayLaterButtonColor.Blue)
        composeTestRule.setContent { PayLaterButtonView(style = color.value, onClick = {}) }

        listOf(PayLaterButtonColor.Blue, PayLaterButtonColor.Black, PayLaterButtonColor.White).forEach {
            color.value = it
            composeTestRule.waitForIdle()
            payLaterButton().assertIsDisplayed().assertIsEnabled()
        }
    }

    @Test
    fun payLaterButtonView_click_invokesOnClick() {
        var clicks = 0
        composeTestRule.setContent { PayLaterButtonView(style = PayLaterButtonColor.Blue, onClick = { clicks++ }) }

        payLaterButton().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun payLaterButtonView_disabled_isNotEnabledAndIgnoresClicks() {
        var clicks = 0
        composeTestRule.setContent {
            PayLaterButtonView(style = PayLaterButtonColor.Blue, enabled = false, onClick = { clicks++ })
        }

        payLaterButton().assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun payLaterButtonView_disabled_showsLoadingIndicatorInPlaceOfLogo() {
        composeTestRule.setContent {
            PayLaterButtonView(style = PayLaterButtonColor.Black, enabled = false, onClick = {})
        }

        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertExists()
    }

    @Test
    fun payLaterButtonView_enabled_doesNotShowLoadingIndicator() {
        composeTestRule.setContent { PayLaterButtonView(style = PayLaterButtonColor.Black, onClick = {}) }

        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertDoesNotExist()
    }

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

    @Test
    fun clickingPayLaterButton_withInvalidAuthorization_reportsFailureAndReenablesButton() {
        var result: PayPalResult? = null
        composeTestRule.setContent {
            PayLaterButton(
                style = PayLaterButtonColor.Blue,
                payPalRequest = payLaterRequest(),
                authorization = "invalid",
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.braintreepayments.demo",
                paypalTokenizeCallback = { result = it }
            )
        }

        payLaterButton().performClick()

        composeTestRule.waitUntil(CLICK_TIMEOUT_MS) { result != null }
        assertTrue(result is PayPalResult.Failure)
        payLaterButton().assertIsEnabled()
    }

    // endregion

    private companion object {
        const val CLICK_TIMEOUT_MS = 5_000L
    }
}
