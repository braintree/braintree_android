package com.braintreepayments.api;


/**
 * Communicates {@link PaymentMethodNonce} from a HTTP request on the main thread.
 * One and only one method will be invoked in response to a request.
 */
public interface PaymentMethodNonceCallback {

    /**
     * @param paymentMethodNonce parsed {@link PaymentMethodNonce} from the HTTP request.
     */
    void success(PaymentMethodNonce paymentMethodNonce);

    /**
     * @param exception error that caused the request to fail.
     */
    void failure(Exception exception);
}
