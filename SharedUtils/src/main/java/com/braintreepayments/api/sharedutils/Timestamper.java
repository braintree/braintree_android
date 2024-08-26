package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Timestamper {

    public long getNow() {
        return System.currentTimeMillis();
    }
}
