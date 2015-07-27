package com.braintreepayments.api.interfaces;

/**
 * Interface that defines a callback for when a configuration error occurs.
 */
public interface ConfigurationErrorListener extends BraintreeListener {

    /**
     * {@link #onConfigurationError(Throwable)} will be called when an error occurred fetching
     * configuration.
     */
    void onConfigurationError(Throwable throwable);
}
