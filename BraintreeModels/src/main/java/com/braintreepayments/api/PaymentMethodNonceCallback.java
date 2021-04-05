package com.braintreepayments.api;


/**
 * Communicates {@link PaymentMethodNonce} from a HTTP request on the main thread.
 * One and only one method will be invoked in response to a request.
 */
public interface PaymentMethodNonceCallback {

    /**
     * @param tokenizationResponse parsed {@link PaymentMethodNonce} from the HTTP request.
     */
    void success(String tokenizationResponse);

    /**
     * @param exception error that caused the request to fail.
     */
    void failure(Exception exception);
}
