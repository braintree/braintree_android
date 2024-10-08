package com.braintreepayments.api.sharedutils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ThreadScheduler implements Scheduler {

    private final Handler mainThreadHandler;
    private final ExecutorService backgroundThreadService;

    ThreadScheduler() {
        this(new Handler(Looper.getMainLooper()), Executors.newCachedThreadPool());
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
