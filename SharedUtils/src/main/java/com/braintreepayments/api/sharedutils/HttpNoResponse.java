package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HttpNoResponse implements HttpResponseCallback {

    @Override
    public void onResult(String responseBody, Exception httpError) {
    }
}
