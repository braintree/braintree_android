package com.braintreepayments.demo.test

import android.content.res.ColorStateList
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.AutomatorAction
import com.braintreepayments.DeviceAutomator
import com.braintreepayments.UiObjectMatcher
import com.braintreepayments.api.uicomponents.PayPalButton
import com.braintreepayments.api.uicomponents.VenmoButton
import com.braintreepayments.demo.R
import com.braintreepayments.demo.test.utilities.TestHelper
import java.util.Objects
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PaymentButtonsColorTest : TestHelper() {
    @Before
    override fun setup() {
        super.setup()
        launchApp()
        DeviceAutomator.onDevice(UiObjectMatcher.withText("Payment Buttons")).waitForEnabled()
            .perform(AutomatorAction.click())
        DeviceAutomator.onDevice(UiObjectMatcher.withResourceId("com.braintreepayments.demo:id/button_pp_blue"))
            .waitForExists()
    }

    private fun getColorFromDrawable(drawable: GradientDrawable): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Objects.requireNonNull<ColorStateList>(drawable.color).defaultColor
        } else {
            try {
                val colorField = GradientDrawable::class.java.getDeclaredField("mFillPaint")
                colorField.isAccessible = true
                val paint = colorField.get(drawable) as Paint?
                return paint?.color ?: 0
            } catch (e: Exception) {
                throw RuntimeException("Failed to get color from GradientDrawable", e)
            }
        }
    }

    private fun getColorFromDrawable(drawable: LayerDrawable): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            /*Assumes knowledge of the internals of the implementation, might be fragile.
             * Layer 0 - Focus drawable
             * Layer 1 - Gradient drawable for the background
             */
            val gradientDrawable = drawable.getDrawable(1) as GradientDrawable
            return Objects.requireNonNull<ColorStateList>(gradientDrawable.color).defaultColor
        } else {
            try {
                val colorField = GradientDrawable::class.java.getDeclaredField("mFillPaint")
                colorField.isAccessible = true
                val gradientDrawable = drawable.getDrawable(1) as GradientDrawable?
                val paint = colorField.get(gradientDrawable) as Paint?
                return paint?.color ?: 0
            } catch (e: Exception) {
                throw RuntimeException("Failed to get color from GradientDrawable", e)
            }
        }
    }

    @Test(timeout = 30000)
    fun testPayPalButton_changesToBlue() {
        Espresso.onView(ViewMatchers.withId(R.id.button_pp_blue)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.pp_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as PayPalButton
                val background = button.background as LayerDrawable
                val actualColor = getColorFromDrawable(background)
                Assert.assertEquals(-0x9f3201, actualColor.toLong())
            })
    }

    @Test(timeout = 30000)
    fun testPayPalButton_changesToBlack() {
        Espresso.onView(ViewMatchers.withId(R.id.button_pp_black)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.pp_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as PayPalButton
                val background = button.background as LayerDrawable
                val actualColor = getColorFromDrawable(background)
                Assert.assertEquals(-0x1000000, actualColor.toLong())
            })
    }

    @Test(timeout = 30000)
    fun testPayPalButton_changesToWhite() {
        Espresso.onView(ViewMatchers.withId(R.id.button_pp_white)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.pp_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as PayPalButton
                val background = button.background as LayerDrawable
                val actualColor = getColorFromDrawable(background)
                Assert.assertEquals(-0x1, actualColor.toLong())
            })
    }

    @Test(timeout = 30000)
    fun testVenmoButton_changesToBlue() {
        Espresso.onView(ViewMatchers.withId(R.id.button_venmo_blue)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.venmo_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as VenmoButton
                val background = button.background as GradientDrawable
                val actualColor = getColorFromDrawable(background)
                Assert.assertEquals(-0xff7301, actualColor.toLong())
            })
    }

    @Test(timeout = 30000)
    fun testVenmoButton_changesToBlack() {
        Espresso.onView(ViewMatchers.withId(R.id.button_venmo_black)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.venmo_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as VenmoButton
                val background = button.background as GradientDrawable
                val actualColor = getColorFromDrawable(background)
                Assert.assertEquals(-0x1000000, actualColor.toLong())
            })
    }

    @Test(timeout = 30000)
    fun testVenmoButton_changesToWhite() {
        Espresso.onView(ViewMatchers.withId(R.id.button_venmo_white)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.venmo_payment_button))
            .check(ViewAssertion { view: View?, noViewFoundException: NoMatchingViewException? ->
                val button = view as VenmoButton
                val background = button.background as GradientDrawable
                val actualColor = getColorFromDrawable(background)
                Assert.assertEquals(-0x1, actualColor.toLong())
            })
    }
}
