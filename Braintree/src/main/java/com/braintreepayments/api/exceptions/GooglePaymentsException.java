package com.braintreepayments.api.exceptions;

import com.google.android.gms.common.api.Status;

/**
 * Error class thrown when an Google Payments exception is encountered.
 */
public class GooglePaymentsException extends BraintreeException {

    private Status mStatus;

    public GooglePaymentsException(String message, Status status) {
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
