package com.braintreepayments.api;

class HttpNoResponse implements HttpResponseCallback {

    @Override
    public void success(String responseBody) {
    }

    @Override
    public void failure(Exception exception) {
    }
}
