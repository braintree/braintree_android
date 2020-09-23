package com.braintreepayments.demo.test.utilities;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.widget.Spinner;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.DeviceAutomator;
import com.braintreepayments.testutils.ExpirationDateHelper;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withClass;
import static com.braintreepayments.UiObjectMatcher.withResourceId;
import static com.braintreepayments.UiObjectMatcher.withText;
import static org.hamcrest.CoreMatchers.equalTo;

public class TestHelper {

    @SuppressLint("ApplySharedPref")
    public void setup() {
        clearPreference("BraintreeApi");
        clearPreference("PayPalOTC");

        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
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
        ApplicationProvider.getApplicationContext().getSharedPreferences(preference, Context.MODE_PRIVATE)
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
        if (ContextCompat.checkSelfPermission(ApplicationProvider.getApplicationContext(), permission.WRITE_EXTERNAL_STORAGE)
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

    protected void fillInExpiration() {
        fillInExpiration("04", ExpirationDateHelper.validExpirationYear());
    }

    protected void fillInExpiration(String month, String year) {
        try {
            onDevice(withText("Expiration Date")).perform(click());
            onDevice(withText(month)).perform(click());
            onDevice(withText(year)).perform(click());
            onDevice().pressBack();
        } catch (RuntimeException e) {
            fillInExpiration();
        }
    }
}
