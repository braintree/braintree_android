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
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;

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

    @Test
    public void threeDSecure_authenticates() {
        onDevice(withText("Card Number")).perform(setText("4000000000001091"));
        fillInExpiration("01", "2022");
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());
    }
}
