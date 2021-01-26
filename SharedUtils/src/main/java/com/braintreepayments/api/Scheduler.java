package com.braintreepayments.api;

public interface Scheduler {
    void runOnMain(Runnable runnable);
    void runOnBackground(Runnable runnable);
}
