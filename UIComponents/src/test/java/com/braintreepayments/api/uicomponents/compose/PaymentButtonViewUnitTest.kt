package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentButtonViewUnitTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `PayPalButtonView shows logo and invokes onClick when enabled`() {
        var clicks = 0
        composeRule.setContent {
            PayPalButtonView(style = PayPalButtonColor.Blue, enabled = true) { clicks++ }
        }

        composeRule.onNodeWithContentDescription("PayPal").assertExists()
        composeRule.onNode(hasClickAction()).assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `PayPalButtonView hides logo and ignores clicks when disabled`() {
        var clicks = 0
        composeRule.setContent {
            PayPalButtonView(style = PayPalButtonColor.Black, enabled = false) { clicks++ }
        }

        composeRule.onNodeWithContentDescription("PayPal").assertDoesNotExist()
        composeRule.onNode(hasClickAction()).assertIsNotEnabled()
        assertEquals(0, clicks)
    }

    @Test
    fun `PayPalButtonView renders every color style`() {
        composeRule.setContent {
            Column {
                PayPalButtonView(style = PayPalButtonColor.Blue) {}
                PayPalButtonView(style = PayPalButtonColor.Black) {}
                PayPalButtonView(style = PayPalButtonColor.White) {}
            }
        }

        composeRule.onAllNodesWithContentDescription("PayPal").assertCountEquals(3)
    }

    @Test
    fun `VenmoButtonView shows logo and invokes onClick when enabled`() {
        var clicks = 0
        composeRule.setContent {
            VenmoButtonView(style = VenmoButtonColor.Blue, enabled = true) { clicks++ }
        }

        composeRule.onNodeWithContentDescription("Venmo").assertExists()
        composeRule.onNode(hasClickAction()).assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `VenmoButtonView hides logo and ignores clicks when disabled`() {
        var clicks = 0
        composeRule.setContent {
            VenmoButtonView(style = VenmoButtonColor.White, enabled = false) { clicks++ }
        }

        composeRule.onNodeWithContentDescription("Venmo").assertDoesNotExist()
        composeRule.onNode(hasClickAction()).assertIsNotEnabled()
        assertEquals(0, clicks)
    }
}
