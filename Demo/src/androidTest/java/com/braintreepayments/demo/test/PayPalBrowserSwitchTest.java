package com.braintreepayments.demo.test;

import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.assumePayPalAppInstalled;
import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.uninstallPayPalWallet;
import static com.braintreepayments.demo.test.utilities.UiTestActions.clickWebViewText;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4.class)
public class PayPalBrowserSwitchTest extends TestHelper {

    @BeforeClass
    public static void uninstallPayPal() {
        uninstallPayPalWallet();
    }

    @Before
    public void setup() {
        super.setup();
        assumePayPalAppInstalled(false);
        onDevice(withText("PayPal")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesASinglePayment() {
        onDevice(withText("Single Payment")).waitForEnabled().perform(click());
        clickWebViewText("Proceed with Sandbox Purchase", 5000);

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesABillingAgreement() {
        onDevice(withText("Billing Agreement")).waitForEnabled().perform(click());
        clickWebViewText("Proceed with Sandbox Purchase", 5000);

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
