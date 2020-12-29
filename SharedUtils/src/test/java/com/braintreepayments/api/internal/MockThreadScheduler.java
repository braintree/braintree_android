package com.braintreepayments.api.internal;

import java.util.ArrayList;
import java.util.List;

class MockThreadScheduler implements Scheduler {

    final private List<Runnable> mainThreadRunnables;
    final private List<Runnable> backgroundThreadRunnables;

    MockThreadScheduler() {
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

    void flushMainThread() {
        List<Runnable> remainingRunnables = new ArrayList<>(mainThreadRunnables);
        mainThreadRunnables.clear();

        for (Runnable runnable: remainingRunnables) {
            runnable.run();
        }
        if (mainThreadRunnables.size() > 0) {
            flushMainThread();
        }
    }

    void flushBackgroundThread() {
        List<Runnable> remainingRunnables = new ArrayList<>(backgroundThreadRunnables);
        backgroundThreadRunnables.clear();

        for (Runnable runnable: remainingRunnables) {
            runnable.run();
        }
        if (backgroundThreadRunnables.size() > 0) {
            flushBackgroundThread();
        }
    }
}
