package com.braintreepayments.demo.test;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.AppInstallationHelper.installPayPalWallet;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PayPalAppSwitchTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        installPayPalWallet();
        onDevice(withText("PayPal")).waitForEnabled().perform(click());
    }

    @Test(timeout = 120000)
    public void appSwitch_forSinglePayment() {
        onDevice(withText("Single Payment")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs("com.paypal.android.p2pmobile");
    }

    @Test(timeout = 120000)
    public void appSwitch_forFuturePayment() {
        onDevice(withText("Future Payment")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs("com.paypal.android.p2pmobile");
    }

    @Test(timeout = 120000)
    public void appSwitch_forFuturePaymentWithAddressScope() {
        onDevice(withText("Future Payment (Address Scope)")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs("com.paypal.android.p2pmobile");
    }

    @Test(timeout = 120000)
    public void appSwitch_forBillingAgreement() {
        onDevice(withText("Billing Agreement")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs("com.paypal.android.p2pmobile");
    }
}
