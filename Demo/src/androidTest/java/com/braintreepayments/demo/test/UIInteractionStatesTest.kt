package com.braintreepayments.demo.test

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.SdkSuppress
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.braintreepayments.AutomatorAction
import com.braintreepayments.DeviceAutomator
import com.braintreepayments.UiObjectMatcher
import com.braintreepayments.demo.R
import com.braintreepayments.demo.test.utilities.TestHelper
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class UIInteractionStatesTest : TestHelper() {

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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun payPalButton_canReceiveFocus() {
        DeviceAutomator.onDevice().pressTab()
        Thread.sleep(100)

        onView(withId(R.id.pp_payment_button)).perform(requestFocus())
        onView(withId(R.id.pp_payment_button)).check(matches(hasFocus()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun venmoButton_canReceiveFocus() {
        DeviceAutomator.onDevice().pressTab()
        Thread.sleep(100)

        onView(withId(R.id.venmo_payment_button)).perform(requestFocus())
        onView(withId(R.id.venmo_payment_button)).check(matches(hasFocus()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun payPalButton_hoverChangesAppearance() {
        var defaultBackgroundColor: Int? = null
        var hoveredBackgroundColor: Int? = null

        // Capture default background color
        onView(withId(R.id.pp_payment_button))
            .perform(captureBackgroundColor { color -> defaultBackgroundColor = color })

        // Hover and capture hovered background color
        onView(withId(R.id.pp_payment_button))
            .perform(hoverAction(enter = true))
            .perform(captureBackgroundColor { color -> hoveredBackgroundColor = color })

        // Verify colors are different
        onView(withId(R.id.pp_payment_button))
            .check(matches(backgroundColorChanged(defaultBackgroundColor, hoveredBackgroundColor)))

        // Exit hover and verify color returns to default
        onView(withId(R.id.pp_payment_button))
            .perform(hoverAction(enter = false))
            .check(matches(hasBackgroundColor(defaultBackgroundColor)))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun venmoButton_hoverChangesAppearance() {
        var defaultBackgroundColor: Int? = null
        var hoveredBackgroundColor: Int? = null

        // Capture default background color
        onView(withId(R.id.venmo_payment_button))
            .perform(captureBackgroundColor { color -> defaultBackgroundColor = color })

        // Hover and capture hovered background color
        onView(withId(R.id.venmo_payment_button))
            .perform(hoverAction(enter = true))
            .perform(captureBackgroundColor { color -> hoveredBackgroundColor = color })

        // Verify colors are different
        onView(withId(R.id.venmo_payment_button))
            .check(matches(backgroundColorChanged(defaultBackgroundColor, hoveredBackgroundColor)))

        // Exit hover and verify color returns to default
        onView(withId(R.id.venmo_payment_button))
            .perform(hoverAction(enter = false))
            .check(matches(hasBackgroundColor(defaultBackgroundColor)))
    }

    private fun requestFocus(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription(): String = "Request focus on view"
            override fun perform(uiController: UiController, view: View) {
                view.requestFocus()
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    private fun hoverAction(enter: Boolean): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription(): String = if (enter) "Hover over view" else "Exit hover from view"

            override fun perform(uiController: UiController, view: View) {
                val location = IntArray(2)
                view.getLocationOnScreen(location)

                val centerX = location[0] + view.width / 2f
                val centerY = location[1] + view.height / 2f
                val downTime = SystemClock.uptimeMillis()

                val action = if (enter) {
                    MotionEvent.ACTION_HOVER_ENTER
                } else {
                    MotionEvent.ACTION_HOVER_EXIT
                }

                val hoverEvent = MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    action,
                    centerX,
                    centerY,
                    0
                )
                hoverEvent.source = InputDevice.SOURCE_MOUSE

                view.dispatchGenericMotionEvent(hoverEvent)
                hoverEvent.recycle()

                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    private fun captureBackgroundColor(onColorCaptured: (Int?) -> Unit): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription(): String = "Capture background color"

            override fun perform(uiController: UiController, view: View) {
                val color = extractBackgroundColor(view)
                onColorCaptured(color)
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    private fun extractBackgroundColor(view: View): Int? {
        val background = view.background
        return when (background) {
            is LayerDrawable -> {
                // PayPalButton and VenmoButton use LayerDrawable with GradientDrawable at index 1
                val gradientDrawable = background.getDrawable(1) as? GradientDrawable
                gradientDrawable?.color?.defaultColor
            }
            is GradientDrawable -> {
                background.color?.defaultColor
            }
            else -> null
        }
    }

    private fun backgroundColorChanged(defaultColor: Int?, hoveredColor: Int?): Matcher<View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("background color changed from $defaultColor to $hoveredColor")
            }

            override fun matchesSafely(view: View): Boolean {
                if (defaultColor == null || hoveredColor == null) {
                    return false
                }
                return defaultColor != hoveredColor
            }
        }
    }

    private fun hasBackgroundColor(expectedColor: Int?): Matcher<View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has background color $expectedColor")
            }

            override fun matchesSafely(view: View): Boolean {
                val actualColor = extractBackgroundColor(view)
                return actualColor == expectedColor
            }
        }
    }
}
