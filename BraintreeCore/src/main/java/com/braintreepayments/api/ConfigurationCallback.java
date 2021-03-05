package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link BraintreeClient#getConfiguration(ConfigurationCallback)}.
 */
public interface ConfigurationCallback {

    /**
     * @param configuration {@link Configuration}
     * @param error an exception that occurred while fetching configuration
     */
    void onResult(@Nullable Configuration configuration, @Nullable Exception error);
}
