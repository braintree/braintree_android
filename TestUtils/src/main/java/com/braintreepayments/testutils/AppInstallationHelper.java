package com.braintreepayments.testutils;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class AppInstallationHelper {

    private static final String PAYPAL_WALLET_PACKAGE_NAME = "com.paypal.android.p2pmobile";

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
                lock.await(90, TimeUnit.SECONDS);
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
                lock.await(30, TimeUnit.SECONDS);
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
