package com.braintreepayments.api;


import androidx.annotation.Nullable;

/**
 * Communicates {@link PaymentMethodNonce} from a HTTP request on the main thread.
 * One and only one method will be invoked in response to a request.
 */
interface TokenizeCallback {

    /**
     * @param tokenizationResult parsed {@link PaymentMethodNonce} from the HTTP request.
     * @param error error that caused the request to fail.
     */
    void onResult(@Nullable TokenizationResult tokenizationResult, @Nullable Exception error);
}
