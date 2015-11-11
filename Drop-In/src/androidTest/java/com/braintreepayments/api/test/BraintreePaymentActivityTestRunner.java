package com.braintreepayments.api.test;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.test.espresso.FailureHandler;
import android.support.test.espresso.base.DefaultFailureHandler;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;

import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.BraintreePaymentTestActivity;
import com.braintreepayments.api.PaymentRequest;
import com.braintreepayments.api.PayPalTestSignatureVerification;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.testutils.ui.ViewHelper;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.setFailureHandler;

@RunWith(AndroidJUnit4.class)
public class BraintreePaymentActivityTestRunner {

    @Rule
    public ActivityTestRule<BraintreePaymentTestActivity> mActivityTestRule =
            new ActivityTestRule<>(BraintreePaymentTestActivity.class, true, false);

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        BraintreeHttpClient.DEBUG = true;
        PayPalTestSignatureVerification.disableAppSwitchSignatureVerification();

        ((KeyguardManager) getTargetContext().getSystemService(Context.KEYGUARD_SERVICE))
                .newKeyguardLock("BraintreePaymentActivity")
                .disableKeyguard();
    }

    public BraintreePaymentActivity getActivity(String clientToken) {
        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext());
        intent.setClass(getTargetContext(), BraintreePaymentTestActivity.class);
        return mActivityTestRule.launchActivity(intent);
    }

    public BraintreePaymentActivity getActivity(String clientToken, long delay) {
        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext());
        intent.setClass(getTargetContext(), BraintreePaymentTestActivity.class)
                .putExtra(BraintreePaymentTestActivity.EXTRA_DELAY, delay);
        return mActivityTestRule.launchActivity(intent);
    }

    public BraintreePaymentActivity getActivity(Intent intent) {
        intent.setClass(getTargetContext(), BraintreePaymentTestActivity.class);
        return mActivityTestRule.launchActivity(intent);
    }

    @Test(timeout = 100)
    public void test(){}
}
