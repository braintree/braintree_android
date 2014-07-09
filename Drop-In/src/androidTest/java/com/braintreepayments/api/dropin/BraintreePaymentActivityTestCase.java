package com.braintreepayments.api.dropin;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.utils.ViewHelper;
import com.google.android.apps.common.testing.ui.espresso.FailureHandler;
import com.google.android.apps.common.testing.ui.espresso.base.DefaultFailureHandler;

import org.hamcrest.Matcher;

import java.io.DataOutputStream;
import java.io.IOException;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.setFailureHandler;

public class BraintreePaymentActivityTestCase extends
        ActivityInstrumentationTestCase2<BraintreePaymentActivity> {

    public BraintreePaymentActivityTestCase() {
        super(BraintreePaymentActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        HttpRequest.DEBUG = true;
        setFailureHandler(new ScreenshotFailureHandler(getInstrumentation(), this));

        KeyguardManager keyguardManager = (KeyguardManager)
                getInstrumentation().getContext().getSystemService(Context.KEYGUARD_SERVICE);
        keyguardManager.newKeyguardLock("BraintreePaymentActivity").disableKeyguard();
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
                String path = "/sdcard/BraintreeUITestScreenshots";
                String testName = mTestCase.getClass().getSimpleName() + "#" + mTestCase.getName() + "-" +
                        System.currentTimeMillis();

                try {
                    Process rootProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", "system/bin/sh"});
                    DataOutputStream stdin = new DataOutputStream(rootProcess.getOutputStream());
                    stdin.writeBytes("mkdir " + path + "\n");
                    stdin.flush();
                    stdin.writeBytes("screencap -p " + path + "/" + testName + ".png\n");
                    stdin.flush();
                    stdin.close();
                } catch (IOException e) {
                    // noop
                }
            }
            mDelegate.handle(error, viewMatcher);
        }
    }
}
