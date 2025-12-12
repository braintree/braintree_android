package com.braintreepayments.demo.test

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.AutomatorAction
import com.braintreepayments.DeviceAutomator.onDevice
import com.braintreepayments.UiObjectMatcher.withResourceId
import com.braintreepayments.UiObjectMatcher.withText
import com.braintreepayments.api.uicomponents.PayPalButton
import com.braintreepayments.api.uicomponents.VenmoButton
import com.braintreepayments.demo.R
import com.braintreepayments.demo.test.utilities.TestHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PaymentButtonsSpinnerTest : TestHelper() {
    @Before
    override fun setup() {
        super.setup()
        launchApp()
        onDevice(withText("Payment Buttons")).waitForEnabled().perform(AutomatorAction.click())
        onDevice(withResourceId("com.braintreepayments.demo:id/button_pp_blue")).waitForExists()
        onDevice(withResourceId("com.braintreepayments.demo:id/button_venmo_blue")).waitForExists()
    }

    @Test(timeout = 30000)
    fun testPayPalButton_isDisabledAndShowsSpinnerAfterClick() {
        onView(withId(R.id.pp_payment_button)).perform(click())

        onView(withId(R.id.pp_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as? PayPalButton
                button?.let {
                    assertFalse(it.isEnabled)

                    val logo = getLogoDrawable(it)
                    assertNotNull(logo)
                    (logo as? Animatable)?.let { animatable ->
                        assertTrue(animatable.isRunning)
                    }
                }
            })
    }

    @Test(timeout = 30000)
    fun testVenmoButton_isDisabledAndShowsSpinnerAfterClick() {
        onView(withId(R.id.venmo_payment_button)).perform(click())

        onView(withId(R.id.venmo_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as? VenmoButton
                button?.let {
                    assertFalse(it.isEnabled)

                    val logo = getLogoDrawable(it)
                    assertNotNull(logo)
                    (logo as? Animatable)?.let { animatable ->
                        assertTrue(animatable.isRunning)
                    }
                }
            })
    }

    private fun getLogoDrawable(button: Any): Drawable? {
        try {
            val logoField = button.javaClass.getDeclaredField("logo")
            logoField.isAccessible = true
            return logoField.get(button) as Drawable?
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get logo field", e)
        }
    }
}
