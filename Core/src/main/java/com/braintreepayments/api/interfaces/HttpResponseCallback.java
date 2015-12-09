package com.braintreepayments.api.interfaces;

import android.support.annotation.MainThread;

/**
 * Communicates responses from a HTTP request on the main thread.
 * One and only one method will be invoked in response to a request.
 */
public interface HttpResponseCallback {

    /**
     * @param responseBody response to the successful HTTP request.
     *        Successful is defined as requests with the response code
     *        {@link java.net.HttpURLConnection#HTTP_OK},
     *        {@link java.net.HttpURLConnection#HTTP_CREATED}
     *        or {@link java.net.HttpURLConnection#HTTP_ACCEPTED}.
     */
    @MainThread
    void success(String responseBody);

    /**
     * @param exception error that caused the request to fail.
     */
    @MainThread
    void failure(Exception exception);
}
