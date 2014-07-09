package com.braintreepayments.api.utils;

import android.app.Activity;

public class TestHelper {

    public static void waitForActivity(Activity activity) {
        TestHelper.waitForActivityWithTimeout(activity, 5000);
    }

    public static void waitForActivityWithTimeout(Activity activity, long timeout) {
        final long endTime = System.currentTimeMillis() + timeout;

        do {
            try {
                if (activity.isFinishing()) {
                    return;
                }
            } catch (Exception e) {
                // noop
            }
        } while (System.currentTimeMillis() < endTime);

        throw new RuntimeException("Maximum wait elapsed (" + timeout + ") while waiting for activity to finish");
    }
}
