package com.braintreepayments.demo.test;

import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.isAppInstalled;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withResourceId;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class VenmoTest extends TestHelper {

    private static final String VENMO_PACKAGE_NAME = "com.venmo";

    @Before
    public void setup() {
        assumeTrue("The Venmo app is not installed.", isAppInstalled(VENMO_PACKAGE_NAME));
    }

    @Test(timeout = 120000)
    public void tokenizesVenmo() {
        onDevice(withText("Venmo")).waitForEnabled().perform(click());
        onDevice(withText("Venmo")).perform(click());
        onDevice(withText("Authorize")).perform(click());

        onDevice(withResourceId("com.braintreepayments.demo:id/nonce"))
                .check(text(containsString("fake-venmo-account-nonce")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
