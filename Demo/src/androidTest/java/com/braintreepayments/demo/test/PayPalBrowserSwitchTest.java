package com.braintreepayments.demo.test;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.AppInstallationHelper.uninstallPayPalWallet;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
@SdkSuppress(minSdkVersion = 21)
public class PayPalBrowserSwitchTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        uninstallPayPalWallet();
        onDevice(withText("PayPal")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesASinglePayment() {
        onDevice(withText("Single Payment")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Proceed with Sandbox Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesAFuturePayment() {
        onDevice(withText("Future Payment")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Email")).perform(click(), setText("test@paypal.com"));
        onDevice().pressDPadDown().typeText("password");
        onDevice(withContentDescription("Log In")).perform(click());
        onDevice(withContentDescription("Agree")).perform(click());

        getNonceDetails().check(text(containsString("Email: jane.doe@example.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesAFuturePaymentWithAddressScope() {
        onDevice(withText("Future Payment (Address Scope)")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Email")).perform(click(), setText("test@paypal.com"));
        onDevice().pressDPadDown().typeText("password");
        onDevice(withContentDescription("Log In")).perform(click());
        onDevice(withContentDescription("Agree")).perform(click());

        getNonceDetails().check(text(containsString("Email: jane.doe@example.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void browserSwitch_makesABillingAgreement() {
        onDevice(withText("Billing Agreement")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Proceed with Sandbox Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
