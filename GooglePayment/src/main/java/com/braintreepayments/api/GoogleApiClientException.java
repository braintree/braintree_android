package com.braintreepayments.api;

/**
 * Exception thrown when the {@link com.google.android.gms.common.api.GoogleApiClient} has an error connecting.
 */
public class GoogleApiClientException extends Exception {

    public enum ErrorType {
        NotAttachedToActivity,
        ConnectionSuspended,
        ConnectionFailed
    }

    private ErrorType mErrorType;
    private int mErrorCode;

    public GoogleApiClientException(ErrorType errorType, int errorCode) {
        mErrorType = errorType;
        mErrorCode = errorCode;
    }

    /**
     * @return The type of error for the error code.
     */
    public ErrorType getErrorType() {
        return mErrorType;
    }

    /**
     * @return The error code returned in {@link com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks}
     * or {@link com.google.android.gms.common.ConnectionResult}.
     */
    public int getErrorCode() {
        return mErrorCode;
    }

    @Override
    public String getMessage() {
        return toString();
    }

    @Override
    public String toString() {
        return getErrorType().name() + ": " + getErrorCode();
    }
}