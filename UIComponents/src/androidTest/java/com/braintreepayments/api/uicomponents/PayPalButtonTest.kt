package com.braintreepayments.api.uicomponents

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PayPalButtonTest {

    @Test
    fun payPalButton_canBeInstantiated() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        assertNotNull(button)
    }

    @Test
    fun payPalButton_isEnabledByDefault() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        assertTrue(button.isEnabled)
    }

    @Test
    fun setButtonColor_toBlack_doesNotThrow() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayPalButtonColor.Black)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toWhite_doesNotThrow() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayPalButtonColor.White)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toSameColor_doesNotThrow() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayPalButtonColor.Blue)
        button.setButtonColor(PayPalButtonColor.Blue)

        assertNotNull(button)
    }

    @Test
    fun setEnabled_false_disablesButton() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.isEnabled = false

        assertFalse(button.isEnabled)
    }

    @Test
    fun setEnabled_trueAfterFalse_reenablesButton() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())
        button.isEnabled = false

        button.isEnabled = true

        assertTrue(button.isEnabled)
    }
}
