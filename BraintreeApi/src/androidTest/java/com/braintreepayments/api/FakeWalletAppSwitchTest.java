package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;

public class FakeWalletAppSwitchTest extends AndroidTestCase {

    private FakeWalletAppSwitch mFakeWalletAppSwitch;

    @Override
    public void setUp(){
        mFakeWalletAppSwitch = new FakeWalletAppSwitch(getContext(), new ClientToken());
    }

    public void testIsAvailableReturnsFalseWhenAppNotInstalled() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        uninstallFakeWallet();
        assertFalse(mFakeWalletAppSwitch.isAvailable());
    }

    public void testIsAvailableReturnsTrueWhenSupportedAppIsInstalled() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        installFakeWallet();
        assertTrue(mFakeWalletAppSwitch.isAvailable());
    }

    private void installFakeWallet() {
        Log.d("request_command", "install fakewallet");
        SystemClock.sleep(60000);
    }

    private void uninstallFakeWallet() {
        Log.d("request_command", "uninstall fakewallet");
        SystemClock.sleep(10000);
    }

    private class FakeWalletAppSwitch extends AppSwitch {
        public FakeWalletAppSwitch(Context context, ClientToken clientToken) {
            super(context, clientToken);
        }

        @Override
        public String getPackage() {
            return "com.braintreepayments.fake.wallet";
        }

        @Override
        protected String getAppSwitchActivity() {
            return "AppSwitchActivity";
        }

        @Override
        public String getCertificateSubject() {
            return "CN=Android Debug,O=Android,C=US";
        }

        @Override
        public String getCertificateIssuer() {
            return "CN=Android Debug,O=Android,C=US";
        }

        @Override
        public int getPublicKeyHashCode() {
            return -1525426004;
        }

        @Override
        public Intent getLaunchIntent() {
            return super.getLaunchIntent()
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        @Override
        public String handleAppSwitchResponse(int requestCode, Intent data) {
            return data.getStringExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE);
        }
    }

}
