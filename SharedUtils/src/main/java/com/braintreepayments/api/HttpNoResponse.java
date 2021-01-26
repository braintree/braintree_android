package com.braintreepayments.api;

public class HttpNoResponse implements HttpResponseCallback {

    @Override
    public void success(String responseBody) {
    }

    @Override
    public void failure(Exception exception) {
    }
}
