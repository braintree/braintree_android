package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that [rememberPayPalButtonColorState] and [rememberVenmoButtonColorState] survive a
 * configuration change (e.g. screen rotation). [StateRestorationTester]
 * emulates the save/restore cycle Android runs on a configuration change.
 */
class ButtonColorStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun paypalButtonColorState_survivesConfigurationChange() {
        val restorationTester = StateRestorationTester(composeTestRule)
        lateinit var style: MutableState<PayPalButtonColor>

        restorationTester.setContent {
            style = rememberPayPalButtonColorState()
        }

        composeTestRule.runOnIdle {
            style.value = PayPalButtonColor.Black
        }

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            assertEquals(PayPalButtonColor.Black, style.value)
        }
    }

    @Test
    fun venmoButtonColorState_survivesConfigurationChange() {
        val restorationTester = StateRestorationTester(composeTestRule)
        lateinit var style: MutableState<VenmoButtonColor>

        restorationTester.setContent {
            style = rememberVenmoButtonColorState()
        }

        composeTestRule.runOnIdle {
            style.value = VenmoButtonColor.White
        }

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            assertEquals(VenmoButtonColor.White, style.value)
        }
    }
}
