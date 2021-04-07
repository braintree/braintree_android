package com.braintreepayments.api;


/**
 * Communicates JSON response from a tokenization request on the main thread.
 */
interface TokenizeCallback {

    /**
     * @param tokenizationResponse parsed {@link BraintreeNonce} from the HTTP request.
     * @param exception error that caused the request to fail.
     */
    void onResult(String tokenizationResponse, Exception exception);
}
