package com.braintreepayments.api.internal;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.braintreepayments.api.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SignatureVerificationTest {

    @Test(timeout = 15000)
    @LargeTest
    public void isSignatureValid_returnsFalseWhenAppNotInstalled() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        Log.d("request_command", "uninstall fakewallet");
        SystemClock.sleep(10000);

        assertFalse(checkSignature());
    }

    @Test(timeout = 65000)
    @LargeTest
    public void isSignatureValid_returnsTrueWhenAppIsInstalled() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        Log.d("request_command", "install fakewallet");
        SystemClock.sleep(60000);

        assertTrue(checkSignature());
    }

    private boolean checkSignature() {
        return SignatureVerification.isSignatureValid(getTargetContext(), "com.braintreepayments.fake.wallet",
                "CN=Android Debug,O=Android,C=US", "CN=Android Debug,O=Android,C=US", 496242318);
    }
}
