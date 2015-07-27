package com.braintreepayments.api.interfaces;

/**
 * Communicates responses from a HTTP request on the main thread.
 * One and only one method will be invoked in response to a request.
 */
public interface HttpResponseCallback {

    /**
     * @param responseBody response to the HTTP request.
     */
    void success(String responseBody);

    /**
     * @param exception error that caused the request to fail.
     */
    void failure(Exception exception);
}
