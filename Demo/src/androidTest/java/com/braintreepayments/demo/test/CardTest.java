package com.braintreepayments.demo.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
import android.widget.ScrollView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.CardNumber;
import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAction.scrollTextIntoView;
import static com.braintreepayments.AutomatorAction.setText;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withClass;
import static com.braintreepayments.UiObjectMatcher.withContentDescription;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@SuppressLint("ApplySharedPref")
@RunWith(AndroidJUnit4ClassRunner.class)
public class CardTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Credit or Debit Cards")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesACard() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("amex_rewards_balance", false)
                .commit();

        onDevice(withText("Card Number")).perform(setText("4111111111111111"));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 11")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Ignore("There is an issue with our merchant account not tokenizing UnionPay. " +
            "Our merchant account can process capabilities and enrollment but " +
            "when tokenizing we get a 422 that the merchant account is not setup for credit card " +
            "processing. " +
            "Disabling UnionPay tests that involve tokenizing until the issue has been resolved.")
    @Test(timeout = 60000)
    public void tokenizesUnionPay() {
        onDevice(withText("Card Number")).perform(setText(CardNumber.UNIONPAY_CREDIT));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
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

    @Ignore("There is an issue with our merchant account not tokenizing UnionPay. " +
            "Our merchant account can process capabilities and enrollment but " +
            "when tokenizing we get a 422 that the merchant account is not setup for credit card " +
            "processing. " +
            "Disabling UnionPay tests that involve tokenizing until the issue has been resolved.")
    @Test(timeout = 60000)
    public void tokenizesUnionPay_whenEnrollmentIsNotRequired() {
        onDevice(withText("Card Number")).perform(setText(CardNumber.UNIONPAY_SMS_NOT_REQUIRED));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVN")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Country Code")).perform(setText("1"));
        onDevice(withText("Mobile Number")).perform(setText("5555555555"));
        onDevice(withText("Send SMS")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 85")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void amexRewardsBalance_whenCardHasBalance() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("amex_rewards_balance", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText("371260714673002"));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CID")).perform(setText("1234"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withTextStartingWith("Amex Rewards Balance:")).check(text(containsString("amount: 45256433")));
        onDevice(withText("OK")).perform(click());
    }

    @Test(timeout = 60000)
    public void amexRewardsBalance_whenCardHasInsufficientPoints() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("amex_rewards_balance", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText("371544868764018"));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CID")).perform(setText("1234"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withTextStartingWith("Amex Rewards Balance:")).check(text(containsString("errorCode: INQ2003")));
        onDevice(withText("OK")).perform(click());
    }

    @Test(timeout = 60000)
    public void amexRewardsBalance_whenCardIsIneligible() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("amex_rewards_balance", true)
                .commit();

        onDevice(withText("Card Number")).perform(setText("378267515471109"));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CID")).perform(setText("1234"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withTextStartingWith("Amex Rewards Balance:")).check(text(containsString("errorCode: INQ2002")));
        onDevice(withText("OK")).perform(click());
    }
}
