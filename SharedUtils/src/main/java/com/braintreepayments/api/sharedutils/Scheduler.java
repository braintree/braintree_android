package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Scheduler {
    void runOnMain(Runnable runnable);
    void runOnBackground(Runnable runnable);
}
