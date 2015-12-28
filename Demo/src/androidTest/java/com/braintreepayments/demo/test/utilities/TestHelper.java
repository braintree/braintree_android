package com.braintreepayments.demo.test.utilities;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Spinner;

import com.braintreepayments.demo.DemoApplication;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withClass;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;

public class TestHelper {

    private static final String PAYPAL_WALLET_PACKAGE_NAME = "com.paypal.android.p2pmobile";

    public void setup() {
        PreferenceManager.getDefaultSharedPreferences(getTargetContext())
                .edit()
                .clear()
                .commit();
        DemoApplication.sIsTest = true;
        onDevice().onHomeScreen().launchApp("com.braintreepayments.demo");
        ensureEnvironmentIs("Sandbox");
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

    public static void installPayPalWallet() {
        if (!isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME)) {
            Log.d("request_command", "install paypal wallet");

            final CountDownLatch lock = new CountDownLatch(1);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        if(isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME)) {
                            lock.countDown();
                            break;
                        }
                    }
                }
            });
            try {
                lock.await(45, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            assertTrue(isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME));
        }
    }

    public static void uninstallPayPalWallet() {
        if (isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME)) {
            Log.d("request_command", "uninstall paypal wallet");

            final CountDownLatch lock = new CountDownLatch(1);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        if(!isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME)) {
                            lock.countDown();
                            break;
                        }
                    }
                }
            });
            try {
                lock.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            assertFalse(isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME));
        }
    }

    public static boolean isAppInstalled(String packageName) {
        PackageManager pm = getTargetContext().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
