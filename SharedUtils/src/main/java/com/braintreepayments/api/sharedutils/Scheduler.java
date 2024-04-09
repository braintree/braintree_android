package com.braintreepayments.api.sharedutils;

interface Scheduler {
    void runOnMain(Runnable runnable);
    void runOnBackground(Runnable runnable);
}
