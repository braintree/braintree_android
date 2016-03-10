package com.braintreepayments.demo.test.utilities;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.widget.Spinner;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withClass;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static org.hamcrest.CoreMatchers.equalTo;

public class TestHelper {

    public void setup() {
        clearPreference("BraintreeApi");
        clearPreference("PayPalOTC");

        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .clear()
                .commit();

        onDevice().onHomeScreen().launchApp("com.braintreepayments.demo");
        enableStoragePermission();
        ensureEnvironmentIs("Sandbox");
    }

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
}
