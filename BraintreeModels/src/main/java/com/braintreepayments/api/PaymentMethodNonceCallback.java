package com.braintreepayments.api;


/**
 * Communicates {@link BraintreeNonce} from a HTTP request on the main thread.
 * One and only one method will be invoked in response to a request.
 */
interface PaymentMethodNonceCallback {

    /**
     * @param braintreeNonce parsed {@link BraintreeNonce} from the HTTP request.
     * @param exception error that caused the request to fail.
     */
    void onResult(BraintreeNonce braintreeNonce, Exception exception);
}
