package com.braintreepayments.demo.test.utilities;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import org.junit.Assume;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

public class AppInstallationHelper {

    public static final String PAYPAL_WALLET_PACKAGE_NAME = "com.paypal.android.p2pmobile";

    public static void installPayPalWallet() {
        if (!isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME)) {
            installApp(PAYPAL_WALLET_PACKAGE_NAME);
        }
    }

    public static void uninstallPayPalWallet() {
        if (isAppInstalled(PAYPAL_WALLET_PACKAGE_NAME)) {
            uninstallApp(PAYPAL_WALLET_PACKAGE_NAME);
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

    public static void installApp(final String packageName) {
        Log.d("request_command", "install " + packageName);

        final CountDownLatch lock = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(isAppInstalled(packageName)) {
                        lock.countDown();
                        break;
                    }
                }
            }
        });
        try {
            lock.await(90, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        assumeTrue(packageName + " is not installed.", isAppInstalled(packageName));
    }

    public static void uninstallApp(final String packageName) {
        Log.d("request_command", "uninstall " + packageName);

        final CountDownLatch lock = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(!isAppInstalled(packageName)) {
                        lock.countDown();
                        break;
                    }
                }
            }
        });
        try {
            lock.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        assumeFalse(packageName + " is installed.", isAppInstalled(packageName));
    }

    public static void assumeAppInstalled(boolean expectedInstalled, String packageName) {
        assumeThat(packageName + " is installed.", expectedInstalled, is(isAppInstalled(packageName)));
    }

    public static void assumePayPalAppInstalled(boolean expectPayPalInstalled) {
        assumeAppInstalled(expectPayPalInstalled, PAYPAL_WALLET_PACKAGE_NAME);
    }
}
