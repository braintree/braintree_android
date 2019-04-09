package com.braintreepayments.demo.test;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.preference.PreferenceManager;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureCardinalTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Credit or Debit Cards")).waitForEnabled().perform(click());

        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();
    }

    @Test(timeout = 40000)
    public void threeDSecure_authenticates() {
        onDevice(withText("Card Number")).perform(setText("4000000000001091"));
        fillInExpiration("01", "2022");
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Code")).perform(setText("1234"));
        onDevice(withText("Submit")).perform(click());

        workAroundForCardinalUiNotClosing();

        getNonceDetails().check(text(containsString("Card Last Two: 91")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: true")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    /**
     * Cardinal SDKs UI does not close on submit.
     * They're working on a fix but in the meantime,
     * pressing back to exit out of the UI works.
     */
    private void workAroundForCardinalUiNotClosing() {
        try {
            // We need to sleep for a bit or Cardinal considers this a cancel
            Thread.sleep(4000);
        } catch (InterruptedException e) {}

        onDevice().pressBack();
    }
}
