package com.braintreepayments.api.interfaces;

/**
 * Interface that defines a callback for {@link com.braintreepayments.api.models.Configuration}.
 */
public interface ConfigurationListener extends BraintreeListener {

    /**
     * {@link #onConfigurationFetched()} will be called when
     * {@link com.braintreepayments.api.models.Configuration} has been successfully fetched.
     */
    void onConfigurationFetched();
}
