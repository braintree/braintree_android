package com.braintreepayments.cardform.test;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

import com.google.android.apps.common.testing.ui.espresso.FailureHandler;
import com.google.android.apps.common.testing.ui.espresso.base.DefaultFailureHandler;

import org.hamcrest.Matcher;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.setFailureHandler;

public class TestActivityTestCase extends ActivityInstrumentationTestCase2<TestActivity> {

    protected Context mContext;
    protected TestActivity mActivity;

    public TestActivityTestCase() {
        super(TestActivity.class);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setUp() throws Exception {
        setFailureHandler(new ScreenshotFailureHandler(getInstrumentation(), this));

        ((KeyguardManager) getInstrumentation().getContext().getSystemService(Context.KEYGUARD_SERVICE))
                .newKeyguardLock("TestActivity").disableKeyguard();

        mContext = getInstrumentation().getContext();
    }

    public void setupCardForm() {
        setupCardForm(true, true, true, true);
    }

    public void setupCardForm(boolean cardNumberRequired, boolean expirationRequired,
            boolean cvvRequired, boolean postalCodeRequired) {
        Intent intent = new Intent(mContext, TestActivity.class)
                .putExtra(TestActivity.CREDIT_CARD, cardNumberRequired)
                .putExtra(TestActivity.EXPIRATION, expirationRequired)
                .putExtra(TestActivity.CVV, cvvRequired)
                .putExtra(TestActivity.POSTAL_CODE, postalCodeRequired);
        setActivityIntent(intent);
        mActivity = getActivity();
    }

    private static class ScreenshotFailureHandler implements FailureHandler {

        private final FailureHandler mDelegate;
        private final ActivityInstrumentationTestCase2<TestActivity> mTestCase;

        public ScreenshotFailureHandler(Instrumentation instrumentation,
                ActivityInstrumentationTestCase2 testCase) {
            mDelegate = new DefaultFailureHandler(instrumentation.getTargetContext());
            mTestCase = testCase;
        }

        @Override
        public void handle(Throwable error, Matcher<View> viewMatcher) {
            Log.d("request_screenshot", mTestCase.getClass().getSimpleName() + "#"
                    + mTestCase.getName() + "-" + System.currentTimeMillis());
            SystemClock.sleep(500);
            mDelegate.handle(error, viewMatcher);
        }
    }

}
