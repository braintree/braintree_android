package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;

class CrashReporting {

    private static final String CRASH_KEY = "com.braintreepayments.api.CrashReporting.CRASH";

    static void persistBraintreeCrash(Context context, Thread thread, Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        if (stringWriter.toString().contains("com.braintreepayments")) {
            BraintreeSharedPreferences.getSharedPreferences(context).edit()
                    .putBoolean(CRASH_KEY, true)
                    .apply();
        }
    }

    static void sendPreviousCrashes(BraintreeFragment fragment) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(fragment.getApplicationContext());
        if (prefs.getBoolean(CRASH_KEY, false)) {
            fragment.sendAnalyticsEvent("crash");
            prefs.edit().putBoolean(CRASH_KEY, false).apply();
        }
    }
}
