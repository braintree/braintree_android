package com.braintreepayments.demo.test;

import android.preference.PreferenceManager;
import android.support.test.runner.AndroidJUnit4;
import android.widget.ScrollView;

import com.braintreepayments.demo.test.utilities.TestHelper;
import com.braintreepayments.testutils.CardNumber;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.scrollTextIntoView;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withClass;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4.class)
public class CardTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Credit or Debit Cards")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesACard() {
        onDevice(withContentDescription("Card Number")).perform(setText("4111111111111111"));
        fillInExpiration();
        onDevice(withContentDescription("CVV")).perform(setText("123"));
        onDevice(withContentDescription("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 11")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void tokenizesUnionPay() {
        onDevice(withContentDescription("Card Number")).perform(setText(CardNumber.UNIONPAY_CREDIT));
        fillInExpiration();
        onDevice(withContentDescription("CVN")).perform(setText("123"));
        onDevice(withContentDescription("Postal Code")).perform(setText("12345"));
        onDevice(withContentDescription("Country Code")).perform(setText("1"));
        onDevice(withContentDescription("Mobile Number")).perform(setText("5555555555"));
        onDevice(withText("Send SMS")).perform(click());

        onDevice(withClass(ScrollView.class)).perform(scrollTextIntoView("SMS Auth Code"));
        onDevice(withContentDescription("SMS Auth Code")).perform(setText("12345"));

        onDevice(withClass(ScrollView.class)).perform(scrollTextIntoView("Purchase"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 32")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void tokenizesUnionPay_whenEnrollmentIsNotRequired() {
        onDevice(withContentDescription("Card Number")).perform(setText(CardNumber.UNIONPAY_SMS_NOT_REQUIRED));
        fillInExpiration();
        onDevice(withContentDescription("CVN")).perform(setText("123"));
        onDevice(withContentDescription("Postal Code")).perform(setText("12345"));
        onDevice(withContentDescription("Country Code")).perform(setText("1"));
        onDevice(withContentDescription("Mobile Number")).perform(setText("5555555555"));
        onDevice(withText("Send SMS")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 85")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void authenticatesWithThreeDSecure() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();

        onDevice(withContentDescription("Card Number")).perform(setText("4000000000000002"));
        fillInExpiration();
        onDevice(withContentDescription("CVV")).perform(setText("123"));
        onDevice(withContentDescription("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();

        onDevice().pressTab();
        onDevice().typeText("1234");
        onDevice().pressTab().pressTab().pressEnter();

        getNonceDetails().check(text(containsString("Card Last Two: 02")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: true")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    private void fillInExpiration() {
        try {
            onDevice(withText("Expiration Date")).perform(click());
            onDevice(withText("12")).perform(click());
            onDevice(withText("2019")).perform(click());
            onDevice().pressBack();
        } catch (RuntimeException e) {
            fillInExpiration();
        }
    }
}
