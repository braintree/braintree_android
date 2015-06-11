package com.braintreepayments.api;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;

import com.braintreepayments.api.internal.SignatureVerification;

public class FakeWalletAppSwitchTest extends AndroidTestCase {

    public void testIsAvailableReturnsFalseWhenAppNotInstalled() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        uninstallFakeWallet();
        assertFalse(checkSignature());
    }

    public void testIsAvailableReturnsTrueWhenSupportedAppIsInstalled() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        installFakeWallet();
        assertTrue(checkSignature());
    }

    private void installFakeWallet() {
        Log.d("request_command", "install fakewallet");
        SystemClock.sleep(60000);
    }

    private void uninstallFakeWallet() {
        Log.d("request_command", "uninstall fakewallet");
        SystemClock.sleep(10000);
    }

    private boolean checkSignature() {
        return SignatureVerification.isSignatureValid(mContext, "com.braintreepayments.fake.wallet",
                "CN=Android Debug,O=Android,C=US", "CN=Android Debug,O=Android,C=US", 496242318);
    }
}
