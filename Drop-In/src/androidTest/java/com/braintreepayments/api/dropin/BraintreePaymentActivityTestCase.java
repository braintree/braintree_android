package com.braintreepayments.api.dropin;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.SystemClock;
import android.support.test.espresso.FailureHandler;
import android.support.test.espresso.base.DefaultFailureHandler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.PayPalTestSignatureVerification;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.internal.VenmoTestSignatureVerification;
import com.braintreepayments.testutils.ui.ViewHelper;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.setFailureHandler;

public class BraintreePaymentActivityTestCase extends
        ActivityInstrumentationTestCase2<BraintreePaymentActivity> {

    protected Context mContext;

    public BraintreePaymentActivityTestCase() {
        super(BraintreePaymentActivity.class);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setUp() throws Exception {
        BraintreeHttpClient.DEBUG = true;
        PayPalTestSignatureVerification.disableAppSwitchSignatureVerification();
        VenmoTestSignatureVerification.disableSignatureVerification();
        setFailureHandler(new ScreenshotFailureHandler(getInstrumentation(), this));

        KeyguardManager keyguardManager = (KeyguardManager)
                getInstrumentation().getContext().getSystemService(Context.KEYGUARD_SERVICE);
        keyguardManager.newKeyguardLock("BraintreePaymentActivity").disableKeyguard();

        mContext = getInstrumentation().getContext();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Braintree.reset();
    }

    private static class ScreenshotFailureHandler implements FailureHandler {

        private final FailureHandler mDelegate;
        private final ActivityInstrumentationTestCase2<BraintreePaymentActivity> mTestCase;

        public ScreenshotFailureHandler(Instrumentation instrumentation,
                ActivityInstrumentationTestCase2 testCase) {
            mDelegate = new DefaultFailureHandler(instrumentation.getTargetContext());
            mTestCase = testCase;
        }

        @Override
        public void handle(Throwable error, Matcher<View> viewMatcher) {
            if (!ViewHelper.sWaitingForView) {
                Log.d("request_screenshot", mTestCase.getClass().getSimpleName() + "#"
                        + mTestCase.getName() + "-" + System.currentTimeMillis());
                SystemClock.sleep(500);
            } else {
                SystemClock.sleep(20);
            }
            mDelegate.handle(error, viewMatcher);
        }
    }

}
