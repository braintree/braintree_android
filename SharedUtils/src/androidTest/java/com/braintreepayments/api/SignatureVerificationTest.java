package com.braintreepayments.api;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@Ignore("This will be removed.")
@RunWith(AndroidJUnit4ClassRunner.class)
public class SignatureVerificationTest {

    private static final String PAYPAL_WALLET_PACKAGE_NAME = "com.paypal.android.p2pmobile";

    @Test(timeout = 20000)
    public void isSignatureValid_returnsFalseWhenAppNotInstalled() {
        Log.d("request_command", "uninstall " + PAYPAL_WALLET_PACKAGE_NAME);
        SystemClock.sleep(15000);
        assertFalse(isAppInstalled());

        assertFalse(checkSignature());
    }

    @Test(timeout = 85000)
    public void isSignatureValid_returnsTrueWhenAppIsInstalled() {
        Log.d("request_command", "install " + PAYPAL_WALLET_PACKAGE_NAME);
        SystemClock.sleep(80000);
        assertTrue(isAppInstalled());

        assertTrue(checkSignature());
    }

    private boolean isAppInstalled() {
        PackageManager pm = ApplicationProvider.getApplicationContext().getPackageManager();
        try {
            pm.getPackageInfo("com.paypal.android.p2pmobile", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private boolean checkSignature() {
        return SignatureVerification.isSignatureValid(ApplicationProvider.getApplicationContext(),
                "com.paypal.android.p2pmobile", "O=Paypal", "O=Paypal", 34172764);
    }
}
