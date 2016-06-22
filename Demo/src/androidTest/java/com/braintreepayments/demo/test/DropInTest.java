package com.braintreepayments.demo.test;

import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextContaining;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DropInTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Drop-In")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesACard() {
        onDevice(withText("Card Number")).perform(setText("4111111111111111"));
        onDevice(withText("Expiration")).perform(setText("1220"));
        onDevice(withTextContaining("BUY")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 11")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @RequiresDevice
    @Test(timeout = 60000)
    public void tokenizesAndroidPay() {
        onDevice(withContentDescription("Pay with Android Pay")).perform(click());
        onDevice(withText("CONTINUE")).perform(click());
        onDevice(withTextContaining("BUY")).perform(click());

        getNonceDetails().check(text(containsString("Underlying Card Last Two")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void exitsAfterCancelingAddingAPaymentMethod() {
        onDevice(withContentDescription("Pay with PayPal")).perform(click());
        onDevice(withContentDescription("Proceed with Sandbox Purchase")).waitForExists();
        onDevice().pressBack();
        onDevice(withContentDescription("Pay with PayPal")).waitForExists();

        onDevice().pressBack();

        onDevice(withText("Drop-In")).check(text(equalToIgnoringCase("Drop-In")));
    }
}
