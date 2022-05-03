package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to provide an asynchronous way for {@link BraintreeClient} to fetch
 * a client token from your server when it is needed. A new client token is only fetched in this way
 * if either an initial value was not passed to {@link BraintreeClient} during instantiation, or if
 * {@link ClientTokenProvider#shouldUseCachedToken()} returns true
 */
public interface ClientTokenProvider {

    /**
     * Method used by {@link BraintreeClient} to fetch a client token. This method is only called if
     * needed (if an initial value was not provided, or if {@link ClientTokenProvider#shouldUseCachedToken()}
     * returns true.
     *
     * @param callback {@link ClientTokenCallback} to invoke to notify {@link BraintreeClient} of success (or failure) when fetching a client token
     */
    void getClientToken(@NonNull ClientTokenCallback callback);

    /**
     * @return true if the {@link BraintreeClient} should use cached client tokens.
     */
    boolean shouldUseCachedToken();
}
