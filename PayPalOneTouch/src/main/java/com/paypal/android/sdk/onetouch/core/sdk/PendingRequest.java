package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Intent;

import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;

public class PendingRequest {

    private final boolean mSuccess;
    private final RequestTarget mRequestTarget;
    private final String mClientMetadataId;
    private final Intent mIntent;

    public PendingRequest(boolean success, RequestTarget requestTarget,
            String clientMetadataId, Intent intent) {
        mSuccess = success;
        mRequestTarget = requestTarget;
        mClientMetadataId = clientMetadataId;
        mIntent = intent;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public RequestTarget getRequestTarget() {
        return mRequestTarget;
    }

    public String getClientMetadataId() {
        return mClientMetadataId;
    }

    public Intent getIntent() {
        return mIntent;
    }
}
