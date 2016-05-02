package com.braintreepayments.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

class CrashReporter implements UncaughtExceptionHandler {

    private BraintreeFragment mBraintreeFragment;
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    static CrashReporter setup(BraintreeFragment fragment) {
        return new CrashReporter(fragment);
    }

    private CrashReporter(BraintreeFragment fragment) {
        mBraintreeFragment = fragment;
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    void tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(mDefaultExceptionHandler);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        if (stringWriter.toString().contains("com.braintreepayments") ||
                stringWriter.toString().contains("com.paypal")) {
            mBraintreeFragment.sendAnalyticsEvent("crash");
        }

        if (mDefaultExceptionHandler != null) {
            mDefaultExceptionHandler.uncaughtException(thread, ex);
        }
    }
}
