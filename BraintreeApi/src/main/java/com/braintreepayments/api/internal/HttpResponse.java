package com.braintreepayments.api.internal;

public class HttpResponse {

    private int mResponseCode;
    private String mResponseBody;

    public HttpResponse(int responseCode, String responseBody) {
        mResponseCode = responseCode;
        mResponseBody = responseBody;
    }

    public int getResponseCode() {
        return mResponseCode;
    }

    public String getResponseBody() {
        return mResponseBody;
    }
}
