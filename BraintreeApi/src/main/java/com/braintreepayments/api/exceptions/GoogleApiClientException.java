package com.braintreepayments.api.exceptions;

/**
 * Exception thrown when the {@link com.google.android.gms.common.api.GoogleApiClient} has an
 * error connecting.
 */
public class GoogleApiClientException extends Exception {
    public GoogleApiClientException(String detailMessage) {
        super(detailMessage);
    }
}
