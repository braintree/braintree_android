package com.paypal.android.sdk.onetouch.core;

/**
 * wrapper for the status of a request.
 */
public class PerformRequestStatus {
    private final boolean mSuccess;
    private final RequestTarget mRequestTarget;
    private final String mClientMetadataId;

    protected PerformRequestStatus(boolean success, RequestTarget requestTarget,
            String clientMetadataId) {
        this.mSuccess = success;
        this.mRequestTarget = requestTarget;
        this.mClientMetadataId = clientMetadataId;
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

    @Override
    public String toString() {
        return PerformRequestStatus.class.getSimpleName()
                + "[mSuccess=" + mSuccess + ", mRequestTarget=" + mRequestTarget +
                ", mClientMetadataId=" + mClientMetadataId + "]";
    }
}
