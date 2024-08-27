package com.braintreepayments.api.testutils;

import com.braintreepayments.api.sharedutils.Scheduler;

import java.util.ArrayList;
import java.util.List;

public class MockThreadScheduler implements Scheduler {

    private final List<Runnable> mainThreadRunnables;
    private final List<Runnable> backgroundThreadRunnables;

    public MockThreadScheduler() {
        mainThreadRunnables = new ArrayList<>();
        backgroundThreadRunnables = new ArrayList<>();
    }

    @Override
    public void runOnMain(Runnable runnable) {
        mainThreadRunnables.add(runnable);
    }

    @Override
    public void runOnBackground(Runnable runnable) {
        backgroundThreadRunnables.add(runnable);
    }

    public void flushMainThread() {
        List<Runnable> remainingRunnables = new ArrayList<>(mainThreadRunnables);
        mainThreadRunnables.clear();

        for (Runnable runnable : remainingRunnables) {
            runnable.run();
        }
        if (mainThreadRunnables.size() > 0) {
            flushMainThread();
        }
    }

    public void flushBackgroundThread() {
        List<Runnable> remainingRunnables = new ArrayList<>(backgroundThreadRunnables);
        backgroundThreadRunnables.clear();

        for (Runnable runnable : remainingRunnables) {
            runnable.run();
        }
        if (backgroundThreadRunnables.size() > 0) {
            flushBackgroundThread();
        }
    }
}
