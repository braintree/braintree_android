package com.braintreepayments.demo.test

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.braintreepayments.AutomatorAction
import com.braintreepayments.DeviceAutomator
import com.braintreepayments.UiObjectMatcher
import com.braintreepayments.demo.R
import com.braintreepayments.demo.test.utilities.TestHelper
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class UIInteractionStatesTest: TestHelper() {

    private lateinit var device: UiDevice

    @Before
    override fun setup() {
        super.setup()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        launchApp()
        DeviceAutomator.onDevice(UiObjectMatcher.withText("Payment Buttons")).waitForEnabled()
            .perform(AutomatorAction.click())
        DeviceAutomator.onDevice(UiObjectMatcher.withResourceId("com.braintreepayments.demo:id/button_pp_blue"))
            .waitForExists()
    }

    @Test
    fun payPalButton_canReceiveFocus() {

        DeviceAutomator.onDevice().pressTab()

        Thread.sleep(100)

        onView(withId(R.id.pp_payment_button)).perform(requestFocusAction())
        onView(withId(R.id.pp_payment_button)).check(matches(hasFocus()))
    }

    @Test
    fun venmoButton_canReceiveFocus() {

        DeviceAutomator.onDevice().pressTab()

        Thread.sleep(100)

        onView(withId(R.id.venmo_payment_button)).perform(requestFocusAction())
        onView(withId(R.id.venmo_payment_button)).check(matches(hasFocus()))
    }

    fun requestFocusAction(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()

            override fun getDescription(): String = "Request focus on view for testing"

            override fun perform(uiController: UiController, view: View) {
                view.requestFocus()
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

}