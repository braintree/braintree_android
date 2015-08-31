package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.exceptions.ErrorWithResponse;

/**
 * Interface that defines callbacks for errors that occur when processing Braintree requests.
 */
public interface BraintreeErrorListener extends BraintreeListener {

    /**
     * onUnrecoverableError will be called where there is an exception that cannot be handled.
     *
     * @param throwable the exception.
     */
    void onUnrecoverableError(Throwable throwable);

    /**
     * onRecoverableError will be called on data validation errors.
     *
     * @param error the validation error.
     */
    void onRecoverableError(ErrorWithResponse error);
}
