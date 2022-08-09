package com.braintreepayments.demo.test;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withResourceId;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.UiObjectMatcher.withTextStartingWith;
import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.isAppInstalled;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class VenmoClientTest extends TestHelper {

    private static final String VENMO_PACKAGE_NAME = "com.venmo";

    @Ignore("Disable until physical device test automation implemented")
    @Test(timeout = 120000)
    public void tokenizesVenmo() {
        launchApp();
        assumeTrue("The Venmo app is not installed.", isAppInstalled(VENMO_PACKAGE_NAME));

        onDevice(withText("Venmo")).waitForEnabled().perform(click());
        onDevice(withText("Venmo")).perform(click());
        onDevice(withText("Authorize")).perform(click());

        onDevice(withResourceId("com.braintreepayments.demo:id/nonce"))
                .check(text(containsString("fake-venmo-account-nonce")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
