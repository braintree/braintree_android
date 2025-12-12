package com.braintreepayments.demo.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withResourceId;
import static com.braintreepayments.UiObjectMatcher.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.AutomatorAction;
import com.braintreepayments.api.uicomponents.PayPalButton;
import com.braintreepayments.api.uicomponents.VenmoButton;
import com.braintreepayments.demo.R;
import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PaymentButtonsSpinnerTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        launchApp();
        onDevice(withText("Payment Buttons")).waitForEnabled().perform(AutomatorAction.click());
        onDevice(withResourceId("com.braintreepayments.demo:id/button_pp_blue")).waitForExists();
        onDevice(withResourceId("com.braintreepayments.demo:id/button_venmo_blue")).waitForExists();
    }

    @Test(timeout = 30000)
    public void testPayPalButton_isDisabledAndShowsSpinnerAfterClick() {
        onView(withId(R.id.pp_payment_button)).perform(click());

        onView(withId(R.id.pp_payment_button)).check((view, noViewFoundException) -> {
            PayPalButton button = (PayPalButton) view;
            assertFalse(view.isEnabled());

            Drawable logo = getLogoDrawable(button);
            assertNotNull(logo);
            assertTrue(logo instanceof Animatable);
            assertTrue(((Animatable) logo).isRunning());
        });
    }

    @Test(timeout = 30000)
    public void testVenmoButton_isDisabledAndShowsSpinnerAfterClick() {
        onView(withId(R.id.venmo_payment_button)).perform(click());

        onView(withId(R.id.venmo_payment_button)).check((view, noViewFoundException) -> {
            VenmoButton button = (VenmoButton) view;
            assertFalse(view.isEnabled());

            Drawable logo = getLogoDrawable(button);
            assertNotNull(logo);
            assertTrue(logo instanceof Animatable);
            assertTrue(((Animatable) logo).isRunning());
        });
    }

    private Drawable getLogoDrawable(Object button) {
        try {
            Field logoField = button.getClass().getDeclaredField("logo");
            logoField.setAccessible(true);
            return (Drawable) logoField.get(button);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get logo field", e);
        }
    }
}