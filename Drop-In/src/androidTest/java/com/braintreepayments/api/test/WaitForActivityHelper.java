package com.braintreepayments.api.test;

import android.app.Activity;

public class WaitForActivityHelper {

    public static void waitForActivityToFinish(Activity activity) {
        WaitForActivityHelper.waitForActivityToFinishWithTimeout(activity, 5000);
    }

    public static void waitForActivityToFinishWithTimeout(Activity activity, long timeout) {
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
