package com.braintreepayments.api;

interface Scheduler {
    void runOnMain(Runnable runnable);
    void runOnBackground(Runnable runnable);
}
