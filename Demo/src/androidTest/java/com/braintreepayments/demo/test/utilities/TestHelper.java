package com.braintreepayments.demo.test.utilities;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.widget.Spinner;

import com.lukekorth.deviceautomator.DeviceAutomator;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withClass;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withResourceId;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static org.hamcrest.CoreMatchers.equalTo;

public class TestHelper {

    @SuppressLint("ApplySharedPref")
    public void setup() {
        clearPreference("BraintreeApi");
        clearPreference("PayPalOTC");

        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .clear()
                .putBoolean("paypal_use_hardcoded_configuration", true)
                .commit();

        onDevice().onHomeScreen().launchApp("com.braintreepayments.demo");
        enableStoragePermission();
        ensureEnvironmentIs("Sandbox");
    }

    public DeviceAutomator getNonceDetails() {
        return onDevice(withResourceId("com.braintreepayments.demo:id/nonce_details"));
    }

    @SuppressLint("ApplySharedPref")
    private void clearPreference(String preference) {
        getTargetContext().getSharedPreferences(preference, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    private static void ensureEnvironmentIs(String environment) {
        try {
            onDevice(withText(environment)).check(text(equalTo(environment)));
        } catch (RuntimeException e) {
            onDevice(withClass(Spinner.class)).perform(click());
            onDevice(withText(environment)).perform(click());
            onDevice(withText(environment)).check(text(equalTo(environment)));
        }
    }

    private static void enableStoragePermission() {
        if (ContextCompat.checkSelfPermission(getTargetContext(), permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            try {
                onDevice(withText("Allow")).perform(click());
            } catch (RuntimeException ignored) {}
        }
    }

    protected static void useTokenizationKey() {
        onDevice(withText("Settings")).perform(click());
        onDevice(withText("Use Tokenization Key")).perform(click());
        onDevice().pressBack();
    }

    protected static void setMerchantAccountId(String merchantAccountId) {
        onDevice(withText("Settings")).perform(click());
        onDevice(withText("Merchant Account")).perform(click());

        onDevice().typeText(merchantAccountId);
        onDevice(withText("OK")).perform(click());
        onDevice().pressBack();
    }
}
