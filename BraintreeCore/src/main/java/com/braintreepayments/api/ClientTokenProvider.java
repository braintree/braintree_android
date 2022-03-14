package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to provide an asynchronous way for {@link BraintreeClient} to fetch
 * a client token from your server when it is needed.
 */
public interface ClientTokenProvider {

    /**
     * Method used by {@link BraintreeClient} to fetch a client token.
     * @param callback {@link ClientTokenCallback} to invoke to notify {@link BraintreeClient} of success (or failure) when fetching a client token
     */
    void getClientToken(@NonNull ClientTokenCallback callback);
}
