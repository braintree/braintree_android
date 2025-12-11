package com.braintreepayments.demo.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withResourceId;
import static com.braintreepayments.UiObjectMatcher.withText;
import static org.junit.Assert.assertEquals;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.AutomatorAction;
import com.braintreepayments.api.uicomponents.PayPalButton;
import com.braintreepayments.api.uicomponents.VenmoButton;
import com.braintreepayments.demo.R;
import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PaymentButtonsColorTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        launchApp();
        onDevice(withText("Payment Buttons")).waitForEnabled().perform(AutomatorAction.click());
        onDevice(withResourceId("com.braintreepayments.demo:id/button_pp_blue")).waitForExists();
    }

    private int getColorFromDrawable(GradientDrawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Objects.requireNonNull(drawable.getColor()).getDefaultColor();
        } else {
            try {
                java.lang.reflect.Field colorField = GradientDrawable.class.getDeclaredField("mFillPaint");
                colorField.setAccessible(true);
                android.graphics.Paint paint = (android.graphics.Paint) colorField.get(drawable);
                return paint != null ? paint.getColor() : 0;
            } catch (Exception e) {
                throw new RuntimeException("Failed to get color from GradientDrawable", e);
            }
        }
    }

    private int getColorFromDrawable(LayerDrawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            /*Assumes knowledge of the internals of the implementation, might be fragile.
             * Layer 0 - Focus drawable
             * Layer 1 - Gradient drawable for the background
             */
            GradientDrawable gradientDrawable = (GradientDrawable) drawable.getDrawable(1);
            return Objects.requireNonNull(gradientDrawable.getColor()).getDefaultColor();
        } else {
            try {
                java.lang.reflect.Field colorField = GradientDrawable.class.getDeclaredField("mFillPaint");
                colorField.setAccessible(true);
                android.graphics.Paint paint = (android.graphics.Paint) colorField.get(drawable);
                return paint != null ? paint.getColor() : 0;
            } catch (Exception e) {
                throw new RuntimeException("Failed to get color from GradientDrawable", e);
            }
        }
    }

    @Test(timeout = 30000)
    public void testPayPalButton_changesToBlue() {
        onView(withId(R.id.button_pp_blue)).perform(click());

        onView(withId(R.id.pp_payment_button)).check((view, noViewFoundException) -> {
            PayPalButton button = (PayPalButton) view;
            LayerDrawable background = (LayerDrawable) button.getBackground();
            int actualColor = getColorFromDrawable(background);
            assertEquals(0xFF60CDFF, actualColor);
        });
    }

    @Test(timeout = 30000)
    public void testPayPalButton_changesToBlack() {
        onView(withId(R.id.button_pp_black)).perform(click());

        onView(withId(R.id.pp_payment_button)).check((view, noViewFoundException) -> {
            PayPalButton button = (PayPalButton) view;
            LayerDrawable background = (LayerDrawable) button.getBackground();
            int actualColor = getColorFromDrawable(background);
            assertEquals(0xFF000000, actualColor);
        });
    }

    @Test(timeout = 30000)
    public void testPayPalButton_changesToWhite() {
        onView(withId(R.id.button_pp_white)).perform(click());

        onView(withId(R.id.pp_payment_button)).check((view, noViewFoundException) -> {
            PayPalButton button = (PayPalButton) view;
            LayerDrawable background = (LayerDrawable) button.getBackground();
            int actualColor = getColorFromDrawable(background);
            assertEquals(0xFFFFFFFF, actualColor);
        });
    }

    @Test(timeout = 30000)
    public void testVenmoButton_changesToBlue() {
        onView(withId(R.id.button_venmo_blue)).perform(click());

        onView(withId(R.id.venmo_payment_button)).check((view, noViewFoundException) -> {
            VenmoButton button = (VenmoButton) view;
            GradientDrawable background = (GradientDrawable) button.getBackground();
            int actualColor = getColorFromDrawable(background);
            assertEquals(0xFF008CFF, actualColor);
        });
    }

    @Test(timeout = 30000)
    public void testVenmoButton_changesToBlack() {
        onView(withId(R.id.button_venmo_black)).perform(click());

        onView(withId(R.id.venmo_payment_button)).check((view, noViewFoundException) -> {
            VenmoButton button = (VenmoButton) view;
            GradientDrawable background = (GradientDrawable) button.getBackground();
            int actualColor = getColorFromDrawable(background);
            assertEquals(0xFF000000, actualColor);
        });
    }

    @Test(timeout = 30000)
    public void testVenmoButton_changesToWhite() {
        onView(withId(R.id.button_venmo_white)).perform(click());

        onView(withId(R.id.venmo_payment_button)).check((view, noViewFoundException) -> {
            VenmoButton button = (VenmoButton) view;
            GradientDrawable background = (GradientDrawable) button.getBackground();
            int actualColor = getColorFromDrawable(background);
            assertEquals(0xFFFFFFFF, actualColor);
        });
    }
}