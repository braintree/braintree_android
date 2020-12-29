package com.braintreepayments.api.internal;

public interface Scheduler {
    void runOnMain(Runnable runnable);
    void runOnBackground(Runnable runnable);
}
