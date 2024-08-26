package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Time {

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
