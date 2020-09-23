package com.braintreepayments.demo.test;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.PAYPAL_WALLET_PACKAGE_NAME;
import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.assumePayPalAppInstalled;
import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.installPayPalWallet;
import static org.hamcrest.Matchers.equalToIgnoringCase;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PayPalAppSwitchTest extends TestHelper {

    @BeforeClass
    public static void installPayPal() {
        installPayPalWallet();
    }

    @Before
    public void setup() {
        super.setup();
        assumePayPalAppInstalled(true);
        onDevice(withText("PayPal")).waitForEnabled().perform(click());
    }

    @Test(timeout = 120000)
    public void appSwitch_forSinglePayment() {
        onDevice(withText("Single Payment")).waitForEnabled().perform(click());

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
        onDevice().pressBack();
        onDevice().pressBack();

        onDevice().checkForegroundAppIs("com.braintreepayments.demo");
        onDevice(withText("Single Payment")).check(text(equalToIgnoringCase("Single Payment")));
    }
}
