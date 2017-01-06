package com.braintreepayments.demo.test;

import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.clearTextField;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;

@RunWith(AndroidJUnit4.class)
public class VisaCheckoutTest extends TestHelper {

    private static final String VISA_CHECKOUT_USERNAME = "test@bt.com";
    private static final String VISA_CHECKOUT_PASSWORD = "12345678";

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
    public void tokenizesVisaCheckout() throws UiObjectNotFoundException {
        onDevice(withContentDescription("Visa Checkout")).perform(click());
        onDevice(withContentDescription("Email or Mobile Number")).perform(click(), clearTextField());

        // TODO bug in DeviceAutomator does not print symbols.
        new UiObject(new UiSelector().descriptionStartsWith("Email or Mobile Number"))
                .setText(VISA_CHECKOUT_USERNAME);
        onDevice(withContentDescription("Password")).perform(click());
        onDevice().typeText(VISA_CHECKOUT_PASSWORD);
        onDevice(withText("Sign In")).perform(click());
        onDevice(withText("Pay with")).waitForExists();
        onDevice(withText("Continue")).perform(click());
        onDevice(withText("Create a Transaction")).waitForExists();
        onDevice(withText("Nonce:")).waitForExists();
    }
}
