package com.braintreepayments.api.test;

import org.robolectric.Robolectric;

import java.util.concurrent.CountDownLatch;

public class RobolectricCountDownLatch {

    private CountDownLatch mCountDownLatch;

    public RobolectricCountDownLatch(int count) {
        mCountDownLatch = new CountDownLatch(count);
    }

    public void await() {
        while (mCountDownLatch.getCount() > 0) {
            Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
            Robolectric.getBackgroundThreadScheduler().advanceToLastPostedRunnable();
        }
    }

    public void countDown() {
        mCountDownLatch.countDown();
    }
}
