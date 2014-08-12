package com.braintreepayments.api.internal;

public class HttpResponse {

    private String mUrl;
    private String mData;

    private int mResponseCode;
    private String mResponseBody;

    public HttpResponse(int responseCode, String responseBody) {
        mResponseCode = responseCode;
        mResponseBody = responseBody;
    }

    protected void setUrl(String url) {
        mUrl = url;
    }

    protected String getUrl() {
        return mUrl;
    }

    protected void setData(String data) {
        mData = data;
    }

    protected String getData() {
        return mData;
    }

    public int getResponseCode() {
        return mResponseCode;
    }

    public String getResponseBody() {
        return mResponseBody;
    }

}
