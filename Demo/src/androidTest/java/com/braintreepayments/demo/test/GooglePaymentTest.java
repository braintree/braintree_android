package com.braintreepayments.demo.test;

import androidx.test.filters.RequiresDevice;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withContentDescription;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4ClassRunner.class)
@RequiresDevice
public class GooglePaymentTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Google Pay")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesGooglePayment() {
        onDevice(withContentDescription("Google Pay")).perform(click());
        onDevice(withText("CONTINUE")).perform(click());

        getNonceDetails().check(text(containsString("Underlying Card Last Two")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
