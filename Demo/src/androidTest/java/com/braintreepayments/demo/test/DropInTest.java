package com.braintreepayments.demo.test;

import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;
import com.braintreepayments.testutils.ExpirationDateHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.uninstallPayPalWallet;
import static com.braintreepayments.demo.test.utilities.UiTestActions.clickWebViewText;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextContaining;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4.class)
public class DropInTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Drop-In")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesACard() {
        onDevice(withText("Credit or Debit Card")).perform(click());
        onDevice(withText("Card Number")).perform(setText(VISA));
        onDevice(withText("12")).perform(click());
        onDevice(withText(ExpirationDateHelper.validExpirationYear())).perform(click());
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice().pressBack();
        onDevice(withTextContaining("Add Card")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 11")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void tokenizesPayPal() {
        uninstallPayPalWallet();

        onDevice(withText("PayPal")).perform(click());
        clickWebViewText("Proceed with Sandbox Purchase", 5000);

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
