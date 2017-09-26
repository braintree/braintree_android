package com.braintreepayments.api.exceptions;

import com.google.android.gms.common.api.Status;

/**
 * Error class thrown when a Google Payment exception is encountered.
 */
public class GooglePaymentException extends BraintreeException {

    private Status mStatus;

    public GooglePaymentException(String message, Status status) {
        super(message);
        mStatus = status;
    }

    /**
     * Get the {@link Status} object that contains more details about the error and how to resolve it.
     *
     * @return {@link Status}
     */
    public Status getStatus() {
        return mStatus;
    }
}
