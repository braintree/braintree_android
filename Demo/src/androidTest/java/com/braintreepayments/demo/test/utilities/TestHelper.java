package com.braintreepayments.demo.test.utilities;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.widget.Spinner;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import com.braintreepayments.DeviceAutomator;
import com.braintreepayments.api.ExpirationDateHelper;
import com.google.android.material.textfield.TextInputLayout;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withClass;
import static com.braintreepayments.UiObjectMatcher.withContentDescription;
import static com.braintreepayments.UiObjectMatcher.withResourceId;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.UiObjectMatcher.withTextContaining;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import java.util.regex.Pattern;

public class TestHelper {

    @SuppressLint("ApplySharedPref")
    public void setup() {
        clearPreference("BraintreeApi");
        clearPreference("com.braintreepayments.api.paypal");

        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
                .edit()
                .clear()
                .putBoolean("paypal_use_hardcoded_configuration", true)
                .commit();

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

    @SuppressLint("ApplySharedPref")
    private void clearPreference(String preference) {
        ApplicationProvider.getApplicationContext().getSharedPreferences(preference, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    private static void ensureEnvironmentIs(String environment) {
        onDevice(withText("Config")).perform(click());

        onDevice(withClass(Spinner.class)).perform(click());
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Ref: https://stackoverflow.com/a/42265283
        // Ref: https://discuss.appium.io/t/how-to-find-android-elements-that-have-the-same-resource-id-but-different-indexs-and-names/25294/6
        // TODO: Refactor UI to get rid of spinners so we won't need complex UiAutomator logic like this
        List<UiObject2> spinnerOptions = uiDevice.findObjects(By.res(Pattern.compile(".*:id/textView")));
        for (UiObject2 option : spinnerOptions) {
            if (environment.equals(option.getText())) {
                option.click();
                break;
            }
        }

        onDevice(withText("Demo")).perform(click());
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

    protected String validExpirationText() {
        String expirationYear = ExpirationDateHelper.validExpirationYear();
        int expirationYearLength = expirationYear.length();
        // format MM/YY
        return "01/" +
            expirationYear.charAt(expirationYearLength - 2)
            + expirationYear.charAt(expirationYearLength - 1);
    }
}
