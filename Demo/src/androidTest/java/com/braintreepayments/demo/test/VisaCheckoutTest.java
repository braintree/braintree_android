package com.braintreepayments.demo.test;

import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;

@RunWith(AndroidJUnit4.class)
public class VisaCheckoutTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        useTokenizationKey();
        onDevice(withText("Visa Checkout")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Visa Checkout")).waitForExists();
    }

    @Test(timeout = 60000)
    public void cancelsVisaCheckout_whenPressingBack() {
        onDevice(withContentDescription("Visa Checkout")).perform(click());
        onDevice().pressBack(); // Dismiss keyboard
        onDevice().pressBack();
        onDevice(withText("OK")).perform(click());
        onDevice(withText("Reset")).waitForExists();
    }

    @Test(timeout = 60000)
    public void cancelsVisaCheckout_whenClickingUpButton() {
        onDevice(withContentDescription("Visa Checkout")).perform(click());
        onDevice(withContentDescription("Back")).perform(click());
        onDevice(withText("OK")).perform(click());
        onDevice(withText("Reset")).waitForExists();
    }

    @Test(timeout = 60000)
    @Ignore("unimplemented")
    public void tokenizesVisaCheckout() {
    }
}
