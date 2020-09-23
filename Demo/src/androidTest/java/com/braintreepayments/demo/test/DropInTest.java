package com.braintreepayments.demo.test;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.demo.test.utilities.TestHelper;
import com.braintreepayments.testutils.ExpirationDateHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAction.setText;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.UiObjectMatcher.withTextContaining;
import static com.braintreepayments.UiObjectMatcher.withTextStartingWith;
import static com.braintreepayments.demo.test.utilities.AppInstallationHelper.uninstallPayPalWallet;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4ClassRunner.class)
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
        onDevice(withText("Proceed with Sandbox Purchase")).waitForExists();
        onDevice(withText("Proceed with Sandbox Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Email: bt_buyer_us@paypal.com")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
