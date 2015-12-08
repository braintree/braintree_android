package com.braintreepayments.api.internal;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SignatureVerificationTest {

    @Test(timeout = 15000)
    public void isSignatureValid_returnsFalseWhenAppNotInstalled() {
        Log.d("request_command", "uninstall paypal wallet");
        SystemClock.sleep(10000);

        assertFalse(checkSignature());
    }

    @Test(timeout = 65000)
    public void isSignatureValid_returnsTrueWhenAppIsInstalled() {
        Log.d("request_command", "install paypal wallet");
        SystemClock.sleep(60000);

        assertTrue(checkSignature());
    }

    private boolean checkSignature() {
        return SignatureVerification.isSignatureValid(getTargetContext(),
                "com.paypal.android.p2pmobile", "O=Paypal", "O=Paypal", 34172764);
    }
}
