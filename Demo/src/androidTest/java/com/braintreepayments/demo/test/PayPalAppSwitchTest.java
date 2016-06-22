package com.braintreepayments.demo.test;

import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.AppInstallationHelper.PAYPAL_WALLET_PACKAGE_NAME;
import static com.braintreepayments.testutils.AppInstallationHelper.installPayPalWallet;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static org.hamcrest.Matchers.equalToIgnoringCase;

@RunWith(AndroidJUnit4.class)
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

        onDevice().checkForegroundAppIs(PAYPAL_WALLET_PACKAGE_NAME);
    }

    @Test(timeout = 120000)
    public void appSwitch_forFuturePayment() {
        onDevice(withText("Future Payment")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs(PAYPAL_WALLET_PACKAGE_NAME);
    }

    @Test(timeout = 120000)
    public void appSwitch_forFuturePaymentWithAddressScope() {
        onDevice(withText("Future Payment (Address Scope)")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs(PAYPAL_WALLET_PACKAGE_NAME);
    }

    @Test(timeout = 120000)
    public void appSwitch_forBillingAgreement() {
        onDevice(withText("Billing Agreement")).waitForEnabled().perform(click());

        onDevice().checkForegroundAppIs(PAYPAL_WALLET_PACKAGE_NAME);
    }

    @Test(timeout = 120000)
    public void appSwitch_postsACancelWhenBackButtonIsPressed() {
        onDevice(withText("Single Payment")).waitForEnabled().perform(click());
        onDevice().checkForegroundAppIs(PAYPAL_WALLET_PACKAGE_NAME);

        onDevice(withText("Email")).waitForExists();
        onDevice().pressBack().pressBack();
        onDevice(withText("OK")).perform(click());

        onDevice().checkForegroundAppIs("com.braintreepayments.demo");
        onDevice(withText("Single Payment")).check(text(equalToIgnoringCase("Single Payment")));
    }
}
