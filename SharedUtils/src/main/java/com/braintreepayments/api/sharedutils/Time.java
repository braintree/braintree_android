package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Time {

    public long getNow() {
        return System.currentTimeMillis();
    }
}
