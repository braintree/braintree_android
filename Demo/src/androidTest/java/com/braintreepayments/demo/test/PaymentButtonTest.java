package com.braintreepayments.demo.test;

import android.support.test.filters.RequiresDevice;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.AppInstallationHelper.uninstallPayPalWallet;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PaymentButtonTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Payment Button")).waitForEnabled().perform(click());
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test(timeout = 60000)
    public void tokenizesPayPal() {
        uninstallPayPalWallet();

        onDevice(withContentDescription("Pay with PayPal")).waitForExists().perform(click());
        onDevice(withContentDescription("Proceed with Sandbox Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @RequiresDevice
    @Test(timeout = 60000)
    public void tokenizesAndroidPay() {
        onDevice(withContentDescription("Pay with Android Pay")).perform(click());
        onDevice(withText("CONTINUE")).perform(click());

        getNonceDetails().check(text(containsString("Underlying Card Last Two")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
