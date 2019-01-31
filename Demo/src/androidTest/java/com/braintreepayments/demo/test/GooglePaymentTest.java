package com.braintreepayments.demo.test;

import androidx.test.filters.RequiresDevice;
import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4.class)
@RequiresDevice
public class GooglePaymentTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Google Pay")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesGooglePayment() {
        onDevice(withText("Pay with Google")).perform(click());
        onDevice(withText("CONTINUE")).perform(click());

        getNonceDetails().check(text(containsString("Underlying Card Last Two")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
