package com.braintreepayments.demo.test;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PayPalBrowserSwitchTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("PayPal")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesASinglePayment() {
        onDevice(withText("Single Payment")).waitForEnabled().perform(click());
        onDevice(withText("Proceed with Sandbox Purchase")).waitForExists();
        onDevice(withText("Proceed with Sandbox Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesABillingAgreement() {
        onDevice(withText("Billing Agreement")).waitForEnabled().perform(click());
        onDevice(withText("Proceed with Sandbox Purchase")).waitForExists();
        onDevice(withText("Proceed with Sandbox Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
