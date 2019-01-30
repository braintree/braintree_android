package com.braintreepayments.demo.test;

import android.annotation.SuppressLint;
import android.preference.PreferenceManager;
import android.support.test.runner.AndroidJUnit4;
import android.widget.ScrollView;

import com.braintreepayments.demo.test.utilities.TestHelper;
import com.braintreepayments.testutils.CardNumber;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.demo.test.utilities.UiTestActions.clickWebViewText;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_AUTHENTICATION_FAILED;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_LOOKUP_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_VERIFICATON;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.scrollTextIntoView;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withClass;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextContaining;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@SuppressLint("ApplySharedPref")
@RunWith(AndroidJUnit4.class)
public class CardTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Credit or Debit Cards")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesACard() {
        onDevice(withText("Card Number")).perform(setText("4111111111111111"));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 11")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void tokenizesUnionPay() {
        onDevice(withText("Card Number")).perform(setText(CardNumber.UNIONPAY_CREDIT));
        fillInExpiration();
        onDevice(withText("CVN")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Country Code")).perform(setText("1"));
        onDevice(withText("Mobile Number")).perform(setText("5555555555"));
        onDevice(withText("Send SMS")).perform(click());

        onDevice(withClass(ScrollView.class)).perform(scrollTextIntoView("Purchase"));
        onDevice(withContentDescription("SMS Auth Code")).perform(setText("12345"));

        onDevice(withClass(ScrollView.class)).perform(scrollTextIntoView("Purchase"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 32")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void tokenizesUnionPay_whenEnrollmentIsNotRequired() {
        onDevice(withText("Card Number")).perform(setText(CardNumber.UNIONPAY_SMS_NOT_REQUIRED));
        fillInExpiration();
        onDevice(withText("CVN")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Country Code")).perform(setText("1"));
        onDevice(withText("Mobile Number")).perform(setText("5555555555"));
        onDevice(withText("Send SMS")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 85")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 80000)
    public void threeDSecure_authenticates() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice().typeText("1234");
        onDevice(withText("Submit")).perform(click());

        ensureThreeDSecureRedirect();

        getNonceDetails().check(text(containsString("Card Last Two: 02")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: true")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 80000)
    public void threeDSecure_authenticationFailed() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_AUTHENTICATION_FAILED));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice().typeText("1234");
        onDevice(withText("Submit")).perform(click());

        ensureThreeDSecureRedirect();

        onDevice(withTextContaining("Failed to authenticate, please try a different form of payment")).waitForExists();
    }

    @Test(timeout = 80000)
    public void threeDSecure_lookupError() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_LOOKUP_ERROR));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 77")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: false")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: false")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void amexRewardsBalance_whenCardHasBalance() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("amex_rewards_balance", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText("371260714673002"));
        fillInExpiration();
        onDevice(withText("CID")).perform(setText("1234"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withTextStartingWith("Amex Rewards Balance:")).check(text(containsString("amount: 45256433")));
    }

    @Test(timeout = 60000)
    public void amexRewardsBalance_whenCardHasInsufficientPoints() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("amex_rewards_balance", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText("371544868764018"));
        fillInExpiration();
        onDevice(withText("CID")).perform(setText("1234"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withTextStartingWith("Amex Rewards Balance:")).check(text(containsString("errorCode: INQ2003")));
    }

    @Test(timeout = 60000)
    public void amexRewardsBalance_whenCardIsIneligible() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("amex_rewards_balance", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText("378267515471109"));
        fillInExpiration();
        onDevice(withText("CID")).perform(setText("1234"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withTextStartingWith("Amex Rewards Balance:")).check(text(containsString("errorCode: INQ2002")));
    }

    private void fillInExpiration() {
        try {
            onDevice(withText("Expiration Date")).perform(click());
            onDevice(withText("04")).perform(click());
            onDevice(withText("2020")).perform(click());
            onDevice().pressBack();
        } catch (RuntimeException e) {
            fillInExpiration();
        }
    }

    private void ensureThreeDSecureRedirect() {
        try {
            clickWebViewText("RETURN TO APP", 3000);
        } catch (RuntimeException ignored) {}
    }
}
