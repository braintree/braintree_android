package com.braintreepayments.demo.test;

import android.preference.PreferenceManager;
import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.demo.test.utilities.TestHelper;
import com.braintreepayments.testutils.CardNumber;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CustomTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Custom")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesACard() {
        onDevice(withText("Card Number")).perform(setText("4111111111111111"));
        onDevice(withText("Expiration")).perform(setText("1220"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 11")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void tokenizesUnionPay() {
        onDevice(withText("Card Number")).perform(setText(CardNumber.UNIONPAY_ENROLLMENT_REQUIRED));
        onDevice(withText("Expiration")).perform(click(), setText("1220"));
        onDevice(withText("Country Code")).perform(setText("1"));
        onDevice(withText("Mobile Phone")).perform(setText("5555555555"));
        onDevice(withText("Send SMS")).perform(click());
        onDevice(withText("SMS Auth Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 17")));

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

    @Test(timeout = 60000)
    public void authenticatesWithThreeDSecure() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText("4000000000000002"));
        onDevice(withText("Expiration")).perform(setText("1220"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();

        onDevice().typeText("\t");
        onDevice().typeText("1234");
        onDevice().typeText("\t");
        onDevice().typeText("\t");
        onDevice().typeText("\n");

        getNonceDetails().check(text(containsString("Card Last Two: 02")));
        getNonceDetails().check(text(containsString("3DS isLiabilityShifted: true")));
        getNonceDetails().check(text(containsString("3DS isLiabilityShiftPossible: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
