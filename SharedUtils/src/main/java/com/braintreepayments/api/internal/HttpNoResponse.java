package com.braintreepayments.api.internal;

import com.braintreepayments.api.interfaces.HttpResponseCallback;

public class HttpNoResponse implements HttpResponseCallback {

    @Override
    public void success(String responseBody) {
    }

    @Override
    public void failure(Exception exception) {
    }
}
