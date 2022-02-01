package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Callback used to communicate {@link AuthorizationProvider#getClientToken(ClientTokenCallback)} result
 * back to {@link BraintreeClient}.
 */
interface ClientTokenCallback {

    /**
     * Invoke this method once a client token has been successfully fetched from the merchant server.
     * @param clientToken Client token fetched from merchant server
     */
    void onSuccess(@NonNull String clientToken);

    /**
     * Invoke this method when an error occurs fetching the client token
     * @param error An error describing the cause of the client token fetch error
     */
    void onFailure(@NonNull Exception error);
}
