package com.braintreepayments.demo.test;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class IdealBrowserSwitchTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        setMerchantAccountId("ideal_eur");
    }

    @Test(timeout = 60000)
    public void browserSwitch_createsAnIdealNonce() {
        onDevice(withText("iDEAL")).waitForEnabled().perform(click());
        onDevice(withText("iDEAL")).waitForEnabled().perform(click());
        onDevice(withText("bunq")).perform(click());
        onDevice(withContentDescription("Continue")).perform(click());

        getNonceDetails().check(text(containsString("COMPLETE")));
    }
}
