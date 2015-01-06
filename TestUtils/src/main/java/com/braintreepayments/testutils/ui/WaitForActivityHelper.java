package com.braintreepayments.testutils.ui;

import android.app.Activity;

public class WaitForActivityHelper {

    public static void waitForActivity(Activity activity) {
        WaitForActivityHelper.waitForActivityWithTimeout(activity, 5000);
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
