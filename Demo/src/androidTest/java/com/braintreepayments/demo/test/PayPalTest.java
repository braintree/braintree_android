package com.braintreepayments.demo.test;

import android.os.SystemClock;
import android.support.test.filters.SdkSuppress;
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
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PayPalTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("PayPal")).waitForEnabled().perform(click());
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test(timeout = 60000)
    public void browserSwitch_makesASinglePayment() {
        uninstallPayPalWallet();

        onDevice(withText("Single Payment")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Proceed with Sandbox Purchase")).perform(click());

        onDevice(withTextContaining("Email:")).check(text(containsString("bt_buyer_us@paypal.com")));
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test(timeout = 60000)
    public void browserSwitch_makesAFuturePayment() {
        uninstallPayPalWallet();

        onDevice(withText("Future Payment")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Email")).perform(click(), setText("test@paypal.com"));
        onDevice().pressDPadDown().typeText("password");
        onDevice(withContentDescription("Log In")).perform(click());
        onDevice(withContentDescription("Agree")).perform(click());

        onDevice(withTextContaining("Email:")).check(text(containsString("jane.doe@example.com")));
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test(timeout = 60000)
    public void browserSwitch_makesAFuturePaymentWithAddressScope() {
        uninstallPayPalWallet();

        onDevice(withText("Future Payment (Address Scope)")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Email")).perform(click(), setText("test@paypal.com"));
        onDevice().pressDPadDown().typeText("password");
        onDevice(withContentDescription("Log In")).perform(click());
        onDevice(withContentDescription("Agree")).perform(click());

        onDevice(withTextContaining("Email:")).check(text(containsString("jane.doe@example.com")));
        SystemClock.sleep(10000);
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test(timeout = 60000)
    public void browserSwitch_makesABillingAgreement() {
        uninstallPayPalWallet();

        onDevice(withText("Billing Agreement")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Proceed with Sandbox Purchase")).perform(click());

        onDevice(withTextContaining("Email:")).check(text(containsString("bt_buyer_us@paypal.com")));
    }

    @Test(timeout = 120000)
    public void appSwitch_forSinglePayment() {
        installPayPalWallet();

        onDevice(withText("Single Payment")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs("com.paypal.android.p2pmobile");
    }

    @Test(timeout = 120000)
    public void appSwitch_forFuturePayment() {
        installPayPalWallet();

        onDevice(withText("Future Payment")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs("com.paypal.android.p2pmobile");
    }

    @Test(timeout = 120000)
    public void appSwitch_forFuturePaymentWithAddressScope() {
        installPayPalWallet();

        onDevice(withText("Future Payment (Address Scope)")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs("com.paypal.android.p2pmobile");
    }

    @Test(timeout = 120000)
    public void appSwitch_usesBrowserForBillingAgreement() {
        installPayPalWallet();

        onDevice(withText("Billing Agreement")).waitForEnabled().perform(click());

        if (isAppInstalled("com.android.chrome")) {
            onDevice().checkForegroundAppIs("com.android.chrome");
        } else {
            onDevice().checkForegroundAppIs("com.android.browser");
        }
    }
}
