package com.braintreepayments.api.sharedutils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ThreadScheduler implements Scheduler {

    // a single thread in the pool is sufficient since we aren't doing a large number
    // of concurrent networking requests
    private final static int POOL_SIZE = 1;

    private final Handler mainThreadHandler;
    private final ExecutorService backgroundThreadService;

    ThreadScheduler() {
        this(new Handler(Looper.getMainLooper()), Executors.newFixedThreadPool(POOL_SIZE));
    }

    @VisibleForTesting
    ThreadScheduler(Handler mainThreadHandler, ExecutorService backgroundThreadPool) {
        this.mainThreadHandler = mainThreadHandler;
        this.backgroundThreadService = backgroundThreadPool;
    }

    public void runOnBackground(Runnable runnable) {
        backgroundThreadService.submit(runnable);
    }

    public void runOnMain(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }
}
