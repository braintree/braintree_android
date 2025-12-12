package com.braintreepayments.demo.test

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isRoot
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
import java.lang.Thread.sleep
import kotlin.text.matches
import org.hamcrest.Matcher
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
        waitForView(withId(R.id.pp_payment_button)).perform(click())

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
        waitForView(withId(R.id.venmo_payment_button)).perform(click())

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

    /**
     * Perform action of implicitly waiting for a certain view.
     * This differs from EspressoExtensions.searchFor in that,
     * upon failure to locate an element, it will fetch a new root view
     * in which to traverse searching for our @param match
     *
     * @param viewMatcher ViewMatcher used to find our view
     */
    fun waitForView(
        viewMatcher: Matcher<View>,
        waitMillis: Int = 5000,
        waitMillisPerTry: Long = 100
    ): ViewInteraction {

        // Derive the max tries
        val maxTries = waitMillis / waitMillisPerTry.toInt()

        var tries = 0

        for (i in 0..maxTries)
            try {
                // Track the amount of times we've tried
                tries++

                // Search the root for the view
                onView(isRoot()).perform(searchFor(viewMatcher))

                // If we're here, we found our view. Now return it
                return onView(viewMatcher)

            } catch (e: Exception) {

                if (tries == maxTries) {
                    throw e
                }
                sleep(waitMillisPerTry)
            }

        throw Exception("Error finding a view matching $viewMatcher")
    }

    private fun searchFor(viewMatcher: Matcher<View>): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                // This action can be performed on any view, but isRoot() is the conventional choice.
                return isRoot()
            }

            override fun getDescription(): String {
                return "searching for view with matcher $viewMatcher"
            }

            override fun perform(uiController: UiController, view: View) {
//                var tries = 0
                val childViews: Iterable<View> = com.braintreepayments.demo.test.TreeIterables.breadthFirst(view)

                // The loop is meant to run until the view is found. The waitForView function
                // already handles the timeout logic, so we just search once per perform call.
                for (child in childViews) {
                    if (viewMatcher.matches(child)) {
                        // View found, so we are done.
                        return
                    }
                }

                // If we reach here, the view was not found in the current hierarchy traversal.
                // The outer loop in waitForView will retry.
            }
        }
    }
}
