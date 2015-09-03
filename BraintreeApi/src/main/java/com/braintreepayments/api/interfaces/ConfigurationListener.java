package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.Configuration;

/**
 * Interface that defines a callback for {@link com.braintreepayments.api.models.Configuration}.
 */
public interface ConfigurationListener extends BraintreeListener {

    /**
     * {@link #onConfigurationFetched(Configuration)} will be called when
     * {@link com.braintreepayments.api.models.Configuration} has been successfully fetched.
     */
    void onConfigurationFetched(Configuration configuration);
}
