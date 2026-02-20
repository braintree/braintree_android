package com.braintreepayments.demo.test.utilities;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.widget.Spinner;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.DeviceAutomator;
import com.braintreepayments.api.testutils.ExpirationDateHelper;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withClass;
import static com.braintreepayments.UiObjectMatcher.withResourceId;
import static com.braintreepayments.UiObjectMatcher.withText;
import static org.hamcrest.CoreMatchers.equalTo;

public class TestHelper {

    public void setup() {
        clearPreference("BraintreeApi");
        clearPreference("com.braintreepayments.api.paypal");

        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
                .edit()
                .clear()
                .putBoolean("paypal_use_hardcoded_configuration", true)
                .apply();

        enableStoragePermission();
    }

    public void launchApp() {
        launchApp("Sandbox");
    }

    public void launchApp(String targetEnvironment) {
        onDevice().onHomeScreen().launchApp("com.braintreepayments.demo");
        ensureEnvironmentIs(targetEnvironment);
    }

    public DeviceAutomator getNonceDetails() {
        return onDevice(withResourceId("com.braintreepayments.demo:id/nonce_details"));
    }

    private void clearPreference(String preference) {
        ApplicationProvider.getApplicationContext().getSharedPreferences(preference, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private static void ensureEnvironmentIs(String environment) {
        onDevice(withClass(Spinner.class)).perform(click());
        onDevice(withText(environment)).perform(click());
        onDevice(withText(environment)).check(text(equalTo(environment)));
    }

    private static void enableStoragePermission() {
        if (ContextCompat.checkSelfPermission(ApplicationProvider.getApplicationContext(), permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            try {
                onDevice(withText("Allow")).perform(click());
            } catch (RuntimeException ignored) {}
        }
    }

    protected String validExpirationText() {
        String expirationYear = ExpirationDateHelper.validExpirationYear();
        int expirationYearLength = expirationYear.length();
        // format MM/YY
        return "01" +
            expirationYear.charAt(expirationYearLength - 2)
            + expirationYear.charAt(expirationYearLength - 1);
    }
}
