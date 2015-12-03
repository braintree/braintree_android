package com.braintreepayments.demo.test;

import android.os.SystemClock;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.demo.test.utilities.TestHelper.clearPreferences;
import static com.braintreepayments.demo.test.utilities.TestHelper.ensureEnvironmentIsSandbox;
import static com.braintreepayments.demo.test.utilities.TestHelper.launchDemoApp;
import static com.braintreepayments.demo.test.utilities.TestHelper.uninstallPayPalWallet;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextContaining;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PaymentButtonTest {

    @Before
    public void setup() {
        clearPreferences();
        launchDemoApp();
        ensureEnvironmentIsSandbox();
        onDevice(withText("Payment Button")).waitForEnabled().perform(click());
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test(timeout = 60000)
    public void tokenizesPayPal() {
        uninstallPayPalWallet();

        SystemClock.sleep(5000);
        onDevice(withContentDescription("Pay with PayPal")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Email")).perform(click(), setText("test@paypal.com"));
        onDevice().pressDPadDown().typeText("password");
        onDevice(withContentDescription("Log In")).perform(click());
        onDevice(withContentDescription("Agree")).perform(click());

        onDevice(withTextContaining("Email:")).check(text(containsString("jane.doe@example.com")));
    }
}
