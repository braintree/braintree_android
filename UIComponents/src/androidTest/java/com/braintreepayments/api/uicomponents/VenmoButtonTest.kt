package com.braintreepayments.api.uicomponents

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoButtonTest {

    @Test
    fun venmoButton_canBeInstantiated() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        assertNotNull(button)
    }

    @Test
    fun venmoButton_isEnabledByDefault() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        assertTrue(button.isEnabled)
    }

    @Test
    fun setButtonColor_toBlack_doesNotThrow() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(VenmoButtonColor.Black)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toWhite_doesNotThrow() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(VenmoButtonColor.White)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toSameColor_doesNotThrow() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(VenmoButtonColor.Blue)
        button.setButtonColor(VenmoButtonColor.Blue)

        assertNotNull(button)
    }

    @Test
    fun setEnabled_false_disablesButton() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.isEnabled = false

        assertFalse(button.isEnabled)
    }

    @Test
    fun setEnabled_trueAfterFalse_reenablesButton() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())
        button.isEnabled = false

        button.isEnabled = true

        assertTrue(button.isEnabled)
    }
}
