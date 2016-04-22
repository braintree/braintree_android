package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.braintreepayments.api.internal.BraintreeSharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

class CrashReporter implements UncaughtExceptionHandler {

    private static final String CRASH_KEY = "com.braintreepayments.api.CrashReporting.CRASH";

    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    static CrashReporter setup(Context context) {
        return new CrashReporter(context);
    }

    private CrashReporter(Context context) {
        mContext = context.getApplicationContext();
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
            BraintreeSharedPreferences.getSharedPreferences(mContext).edit()
                    .putBoolean(CRASH_KEY, true)
                    .apply();
        }

        if (mDefaultExceptionHandler != null) {
            mDefaultExceptionHandler.uncaughtException(thread, ex);
        }
    }

    void sendPreviousCrashes(BraintreeFragment fragment) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(mContext);
        if (prefs.getBoolean(CRASH_KEY, false)) {
            fragment.sendAnalyticsEvent("crash");
            prefs.edit().putBoolean(CRASH_KEY, false).apply();
        }
    }
}
